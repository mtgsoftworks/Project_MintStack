package com.mintstack.finance.service.market;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BondMaturityBootstrapService {

    private final BistDataStoreMarketDataService bistDataStoreMarketDataService;

    @Async("taskExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void enrichMissingBondMaturityOnStartup() {
        try {
            BistDataStoreMarketDataService.BondMaturityEnrichmentResult result =
                bistDataStoreMarketDataService.enrichBondMaturityMetadata();
            if (result.parsedFromSymbols() > 0 || result.enrichedFromBulletins() > 0) {
                log.info(
                    "Bond maturity startup enrichment completed: symbolParsed={}, bulletinEnriched={}",
                    result.parsedFromSymbols(),
                    result.enrichedFromBulletins()
                );
            }
        } catch (Exception error) {
            log.warn("Bond maturity startup enrichment failed: {}", error.getMessage());
        }
    }
}
