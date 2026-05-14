package com.mintstack.finance.service.market;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.service.external.TefasFundClient;
import com.mintstack.finance.service.external.TefasFundClient.TefasFundPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TefasFundDataService {

    private final TefasFundClient tefasFundClient;
    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Value("${app.external-api.tefas.enabled:true}")
    private boolean enabled;

    @Transactional
    public int refreshFundPrices() {
        if (!enabled) {
            log.debug("TEFAS integration disabled. Skipping fund refresh.");
            return 0;
        }

        List<TefasFundPrice> prices = tefasFundClient.fetchLatestFundPrices();
        prices.forEach(this::upsertFundPrice);
        log.info("TEFAS fund refresh completed: {} prices processed", prices.size());
        return prices.size();
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
        instrument.setPreviousClose(instrument.getCurrentPrice());
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
