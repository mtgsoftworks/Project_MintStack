package com.mintstack.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Main application class for MintStack Finance Portal.
 * 
 * This application provides:
 * - Financial market data (currencies, stocks, bonds, funds, VIOP)
 * - News aggregation for financial markets
 * - Portfolio management with profit/loss tracking
 * - Technical analysis tools
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class FinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
