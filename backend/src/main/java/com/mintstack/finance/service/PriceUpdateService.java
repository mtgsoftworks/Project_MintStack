package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.PriceUpdateMessage;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for broadcasting real-time price updates via WebSocket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceUpdateService {

    private final ClusterWebSocketPublisher webSocketPublisher;
    
    // Lazy to break circular dependency: AlertService -> PriceUpdateService -> AlertService
    @Lazy
    private final AlertService alertService;

    /**
     * Cache for tracking last broadcast prices to avoid duplicate updates
     */
    private final Map<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();

    /**
     * Broadcast a currency rate update to all subscribers
     */
    @Observed(name = "ws.broadcast.currency", contextualName = "broadcast-currency-update")
    public void broadcastCurrencyUpdate(String currencyCode, BigDecimal buyingRate, 
                                        BigDecimal sellingRate) {
        PriceUpdateMessage message = PriceUpdateMessage.builder()
                .type("CURRENCY")
                .symbol(currencyCode)
                .price(buyingRate)
                .previousPrice(lastPrices.get("CURRENCY_" + currencyCode))
                .additionalData(Map.of(
                    "buyingRate", buyingRate,
                    "sellingRate", sellingRate
                ))
                .timestamp(LocalDateTime.now())
                .build();

        lastPrices.put("CURRENCY_" + currencyCode, buyingRate);
        sendMessage("/topic/prices/currency", message);
        sendMessage("/topic/prices/currency/" + currencyCode, message);
        
        log.debug("Broadcast currency update: {} = {}", currencyCode, buyingRate);
        
        // Check and trigger price alerts for currency updates
        try {
            alertService.checkAlertsForSymbol(currencyCode, buyingRate);
        } catch (Exception e) {
            log.warn("Error checking alerts for currency {}: {}", currencyCode, e.getMessage());
        }
    }

    /**
     * Broadcast a stock price update to all subscribers
     */
    @Observed(name = "ws.broadcast.stock", contextualName = "broadcast-stock-update")
    public void broadcastStockUpdate(String symbol, BigDecimal currentPrice, 
                                     BigDecimal previousClose, BigDecimal change,
                                     BigDecimal changePercent) {
        PriceUpdateMessage message = PriceUpdateMessage.builder()
                .type("STOCK")
                .symbol(symbol)
                .price(currentPrice)
                .previousPrice(lastPrices.get("STOCK_" + symbol))
                .additionalData(Map.of(
                    "previousClose", previousClose != null ? previousClose : BigDecimal.ZERO,
                    "change", change != null ? change : BigDecimal.ZERO,
                    "changePercent", changePercent != null ? changePercent : BigDecimal.ZERO
                ))
                .timestamp(LocalDateTime.now())
                .build();

        lastPrices.put("STOCK_" + symbol, currentPrice);
        sendMessage("/topic/prices/stocks", message);
        sendMessage("/topic/prices/stocks/" + symbol, message);
        
        log.debug("Broadcast stock update: {} = {}", symbol, currentPrice);
        
        // ADDED: Check and trigger price alerts after each stock update
        try {
            alertService.checkAlertsForSymbol(symbol, currentPrice);
        } catch (Exception e) {
            log.warn("Error checking alerts for {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Broadcast a cryptocurrency price update to all subscribers
     */
    @Observed(name = "ws.broadcast.crypto", contextualName = "broadcast-crypto-update")
    public void broadcastCryptoUpdate(String symbol, BigDecimal currentPrice, 
                                       BigDecimal previousClose, BigDecimal changePercent24h,
                                       BigDecimal high24h, BigDecimal low24h, Long volume24h) {
        PriceUpdateMessage message = PriceUpdateMessage.builder()
                .type("CRYPTO")
                .symbol(symbol)
                .price(currentPrice)
                .previousPrice(lastPrices.get("CRYPTO_" + symbol))
                .additionalData(Map.of(
                    "previousClose", previousClose != null ? previousClose : BigDecimal.ZERO,
                    "changePercent24h", changePercent24h != null ? changePercent24h : BigDecimal.ZERO,
                    "high24h", high24h != null ? high24h : BigDecimal.ZERO,
                    "low24h", low24h != null ? low24h : BigDecimal.ZERO,
                    "volume24h", volume24h != null ? volume24h : 0L
                ))
                .timestamp(LocalDateTime.now())
                .build();

        lastPrices.put("CRYPTO_" + symbol, currentPrice);
        sendMessage("/topic/prices/crypto", message);
        sendMessage("/topic/prices/crypto/" + symbol, message);
        
        log.debug("Broadcast crypto update: {} = {}", symbol, currentPrice);
        
        try {
            alertService.checkAlertsForSymbol(symbol, currentPrice);
        } catch (Exception e) {
            log.warn("Error checking alerts for crypto {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Broadcast a general market update
     */
    @Observed(name = "ws.broadcast.market", contextualName = "broadcast-market-update")
    public void broadcastMarketUpdate(String type, String symbol, BigDecimal price,
                                      Map<String, Object> additionalData) {
        PriceUpdateMessage message = PriceUpdateMessage.builder()
                .type(type)
                .symbol(symbol)
                .price(price)
                .previousPrice(lastPrices.get(type + "_" + symbol))
                .additionalData(additionalData)
                .timestamp(LocalDateTime.now())
                .build();

        lastPrices.put(type + "_" + symbol, price);
        sendMessage("/topic/prices", message);
        
        log.debug("Broadcast market update: {} {} = {}", type, symbol, price);

        if (price != null) {
            try {
                alertService.checkAlertsForSymbol(symbol, price);
            } catch (Exception e) {
                log.warn("Error checking alerts for {} {}: {}", type, symbol, e.getMessage());
            }
        }
    }

    /**
     * Send a message to a specific destination
     */
    private void sendMessage(String destination, PriceUpdateMessage message) {
        try {
            webSocketPublisher.broadcast(destination, message);
        } catch (Exception e) {
            log.error("Error sending WebSocket message to {}: {}", destination, e.getMessage());
        }
    }

    /**
     * Send a private message to a specific user
     */
    @Observed(name = "ws.broadcast.user", contextualName = "send-user-message")
    public void sendToUser(String userId, String destination, Object message) {
        try {
            webSocketPublisher.broadcastToUser(userId, destination, message);
        } catch (Exception e) {
            log.error("Error sending private message to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Clear the price cache (useful for testing)
     */
    public void clearPriceCache() {
        lastPrices.clear();
    }
}
