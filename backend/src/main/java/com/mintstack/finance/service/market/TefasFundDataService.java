package com.mintstack.finance.service.market;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.service.PriceUpdateService;
import com.mintstack.finance.service.external.TefasFundClient;
import com.mintstack.finance.service.external.TefasFundClient.TefasFundPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TefasFundDataService {

    private final TefasFundClient tefasFundClient;
    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PriceUpdateService priceUpdateService;

    @Value("${app.external-api.tefas.enabled:true}")
    private boolean enabled;

    @Value("${app.market-data.max-active-fund-instruments:500}")
    private int maxActiveFundInstruments;

    @Transactional
    public int refreshFundPrices() {
        if (!enabled) {
            log.debug("TEFAS integration disabled. Skipping fund refresh.");
            return 0;
        }

        List<TefasFundPrice> prices = tefasFundClient.fetchLatestFundPrices();
        if (prices.isEmpty()) {
            log.warn("TEFAS fund refresh returned empty price list; keeping existing active funds.");
            return 0;
        }

        int cap = resolveFundCap();
        List<TefasFundPrice> ranked = prices.stream()
            .sorted(Comparator
                .comparing(TefasFundPrice::portfolioSize, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                .thenComparing(TefasFundPrice::investorCount, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                .thenComparing(TefasFundPrice::sharesOutstanding, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                .thenComparing(TefasFundPrice::fundCode))
            .toList();

        List<TefasFundPrice> selected = ranked.stream()
            .limit(cap)
            .toList();
        Set<String> selectedSymbols = selected.stream()
            .map(TefasFundPrice::fundCode)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        selected.forEach(this::upsertFundPrice);
        deactivateFundsOutsideSelection(selectedSymbols);

        log.info("TEFAS fund refresh completed: {} selected / {} fetched (active cap={})",
            selected.size(), prices.size(), cap);
        return selected.size();
    }

    private void upsertFundPrice(TefasFundPrice fundPrice) {
        Instrument instrument = instrumentRepository.findBySymbol(fundPrice.fundCode())
            .orElseGet(() -> Instrument.builder()
                .symbol(fundPrice.fundCode())
                .name(fundPrice.fundName())
                .type(Instrument.InstrumentType.FUND)
                .exchange("TEFAS")
                .currency("TRY")
                .isActive(true)
                .isSimulated(false)
                .build());

        instrument.setName(fundPrice.fundName());
        instrument.setType(Instrument.InstrumentType.FUND);
        instrument.setExchange("TEFAS");
        instrument.setCurrency("TRY");
        instrument.setIsActive(true);
        instrument.setIsSimulated(false);
        instrument.setPreviousClose(resolvePreviousCloseForDate(instrument, fundPrice.date()));
        instrument.setCurrentPrice(fundPrice.price());
        Instrument saved = instrumentRepository.save(instrument);

        PriceHistory history = priceHistoryRepository
            .findByInstrumentIdAndPriceDate(saved.getId(), fundPrice.date())
            .orElseGet(() -> PriceHistory.builder()
                .instrument(saved)
                .priceDate(fundPrice.date())
                .build());
        history.setOpenPrice(firstPositive(fundPrice.exchangeBulletinPrice(), fundPrice.price()));
        history.setHighPrice(fundPrice.price());
        history.setLowPrice(fundPrice.price());
        history.setClosePrice(fundPrice.price());
        history.setAdjustedClose(fundPrice.price());
        history.setVolume(toLong(fundPrice.sharesOutstanding()));
        priceHistoryRepository.save(history);

        Map<String, Object> additionalData = new HashMap<>();
        if (fundPrice.date() != null) {
            additionalData.put("tradeDate", fundPrice.date());
        }
        if (fundPrice.portfolioSize() != null) {
            additionalData.put("portfolioSize", fundPrice.portfolioSize());
        }
        if (fundPrice.investorCount() != null) {
            additionalData.put("investorCount", fundPrice.investorCount());
        }
        priceUpdateService.broadcastMarketUpdate("FUND", saved.getSymbol(), fundPrice.price(), additionalData);
    }

    private BigDecimal resolvePreviousCloseForDate(Instrument instrument, LocalDate effectiveDate) {
        if (instrument == null) {
            return null;
        }

        if (instrument.getId() != null && effectiveDate != null) {
            List<PriceHistory> recent = priceHistoryRepository.findByInstrumentIdOrderByPriceDateDesc(
                instrument.getId(),
                PageRequest.of(0, 20)
            );
            for (PriceHistory item : recent) {
                if (item.getPriceDate() == null || !item.getPriceDate().isBefore(effectiveDate)) {
                    continue;
                }
                BigDecimal close = firstPositive(item.getClosePrice(), item.getAdjustedClose(), item.getOpenPrice());
                if (close != null) {
                    return close;
                }
            }
        }

        return firstPositive(instrument.getCurrentPrice(), instrument.getPreviousClose());
    }

    private void deactivateFundsOutsideSelection(Set<String> selectedSymbols) {
        if (selectedSymbols == null || selectedSymbols.isEmpty()) {
            return;
        }

        List<Instrument> activeFunds = instrumentRepository.findByTypeAndIsActiveTrue(Instrument.InstrumentType.FUND);
        List<Instrument> toDeactivate = activeFunds.stream()
            .filter(fund -> fund.getSymbol() != null && !selectedSymbols.contains(fund.getSymbol()))
            .peek(fund -> fund.setIsActive(false))
            .toList();

        if (!toDeactivate.isEmpty()) {
            instrumentRepository.saveAll(toDeactivate);
            log.info("TEFAS fund universe trimmed: {} funds deactivated outside top selection", toDeactivate.size());
        }
    }

    private int resolveFundCap() {
        return Math.max(50, Math.min(2000, maxActiveFundInstruments));
    }

    private BigDecimal firstPositive(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null && value.signum() > 0) {
                return value;
            }
        }
        return null;
    }

    private Long toLong(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
            return null;
        }
        return value.longValue();
    }
}
