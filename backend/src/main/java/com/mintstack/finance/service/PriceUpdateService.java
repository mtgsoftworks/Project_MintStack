package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.PriceUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Cache for tracking last broadcast prices to avoid duplicate updates
     */
    private final Map<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();

    /**
     * Broadcast a currency rate update to all subscribers
     */
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
    }

    /**
     * Broadcast a stock price update to all subscribers
     */
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
    }

    /**
     * Broadcast a general market update
     */
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
    }

    /**
     * Send a message to a specific destination
     */
    private void sendMessage(String destination, PriceUpdateMessage message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("Error sending WebSocket message to {}: {}", destination, e.getMessage());
        }
    }

    /**
     * Send a private message to a specific user
     */
    public void sendToUser(String userId, String destination, Object message) {
        try {
            messagingTemplate.convertAndSendToUser(userId, destination, message);
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
