package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.PriceUpdateMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceUpdateServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private PriceUpdateService priceUpdateService;

    @Test
    void broadcastCurrencyUpdate_ShouldSendToCorrectTopics() {
        // Given
        String currencyCode = "USD";
        BigDecimal buyingRate = BigDecimal.valueOf(34.50);
        BigDecimal sellingRate = BigDecimal.valueOf(34.70);

        // When
        priceUpdateService.broadcastCurrencyUpdate(currencyCode, buyingRate, sellingRate);

        // Then
        verify(messagingTemplate).convertAndSend(eq("/topic/prices/currency"), any(PriceUpdateMessage.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/prices/currency/USD"), any(PriceUpdateMessage.class));
    }

    @Test
    void broadcastCurrencyUpdate_ShouldContainCorrectData() {
        // Given
        String currencyCode = "EUR";
        BigDecimal buyingRate = BigDecimal.valueOf(36.00);
        BigDecimal sellingRate = BigDecimal.valueOf(36.20);
        
        ArgumentCaptor<PriceUpdateMessage> captor = ArgumentCaptor.forClass(PriceUpdateMessage.class);

        // When
        priceUpdateService.broadcastCurrencyUpdate(currencyCode, buyingRate, sellingRate);

        // Then
        verify(messagingTemplate, atLeast(1)).convertAndSend(eq("/topic/prices/currency"), captor.capture());
        PriceUpdateMessage message = captor.getValue();
        
        assertThat(message.getType()).isEqualTo("CURRENCY");
        assertThat(message.getSymbol()).isEqualTo("EUR");
        assertThat(message.getPrice()).isEqualTo(buyingRate);
    }

    @Test
    void broadcastStockUpdate_ShouldSendToCorrectTopics() {
        // Given
        String symbol = "THYAO";
        BigDecimal currentPrice = BigDecimal.valueOf(145.50);
        BigDecimal previousClose = BigDecimal.valueOf(142.00);
        BigDecimal change = BigDecimal.valueOf(3.50);
        BigDecimal changePercent = BigDecimal.valueOf(2.46);

        // When
        priceUpdateService.broadcastStockUpdate(symbol, currentPrice, previousClose, change, changePercent);

        // Then
        verify(messagingTemplate).convertAndSend(eq("/topic/prices/stocks"), any(PriceUpdateMessage.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/prices/stocks/THYAO"), any(PriceUpdateMessage.class));
    }

    @Test
    void broadcastStockUpdate_ShouldContainCorrectData() {
        // Given
        String symbol = "GARAN";
        BigDecimal currentPrice = BigDecimal.valueOf(50.20);
        BigDecimal previousClose = BigDecimal.valueOf(49.00);
        BigDecimal change = BigDecimal.valueOf(1.20);
        BigDecimal changePercent = BigDecimal.valueOf(2.45);

        ArgumentCaptor<PriceUpdateMessage> captor = ArgumentCaptor.forClass(PriceUpdateMessage.class);

        // When
        priceUpdateService.broadcastStockUpdate(symbol, currentPrice, previousClose, change, changePercent);

        // Then
        verify(messagingTemplate, atLeast(1)).convertAndSend(eq("/topic/prices/stocks"), captor.capture());
        PriceUpdateMessage message = captor.getValue();
        
        assertThat(message.getType()).isEqualTo("STOCK");
        assertThat(message.getSymbol()).isEqualTo("GARAN");
        assertThat(message.getPrice()).isEqualTo(currentPrice);
    }

    @Test
    void broadcastMarketUpdate_ShouldSendToGeneralTopic() {
        // Given
        String type = "BOND";
        String symbol = "TRY10Y";
        BigDecimal price = BigDecimal.valueOf(25.50);
        Map<String, Object> additionalData = Map.of("yield", 25.5);

        // When
        priceUpdateService.broadcastMarketUpdate(type, symbol, price, additionalData);

        // Then
        verify(messagingTemplate).convertAndSend(eq("/topic/prices"), any(PriceUpdateMessage.class));
    }

    @Test
    void sendToUser_ShouldSendPrivateMessage() {
        // Given
        String userId = "user-123";
        String destination = "/queue/alerts";
        Object message = Map.of("alert", "triggered");

        // When
        priceUpdateService.sendToUser(userId, destination, message);

        // Then
        verify(messagingTemplate).convertAndSendToUser(userId, destination, message);
    }

    @Test
    void clearPriceCache_ShouldClearCache() {
        // Given
        priceUpdateService.broadcastCurrencyUpdate("USD", BigDecimal.valueOf(34.50), BigDecimal.valueOf(34.70));

        // When
        priceUpdateService.clearPriceCache();

        // Then - subsequent update should not have previous price
        ArgumentCaptor<PriceUpdateMessage> captor = ArgumentCaptor.forClass(PriceUpdateMessage.class);
        priceUpdateService.broadcastCurrencyUpdate("USD", BigDecimal.valueOf(35.00), BigDecimal.valueOf(35.20));
        
        verify(messagingTemplate, atLeast(2)).convertAndSend(eq("/topic/prices/currency"), captor.capture());
        PriceUpdateMessage lastMessage = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastMessage.getPreviousPrice()).isNull();
    }

    @Test
    void broadcastCurrencyUpdate_ShouldTrackPreviousPrice() {
        // Given
        String currencyCode = "GBP";
        BigDecimal firstPrice = BigDecimal.valueOf(42.00);
        BigDecimal secondPrice = BigDecimal.valueOf(42.50);

        // When - first update
        priceUpdateService.broadcastCurrencyUpdate(currencyCode, firstPrice, firstPrice.add(BigDecimal.valueOf(0.20)));
        
        // When - second update
        ArgumentCaptor<PriceUpdateMessage> captor = ArgumentCaptor.forClass(PriceUpdateMessage.class);
        priceUpdateService.broadcastCurrencyUpdate(currencyCode, secondPrice, secondPrice.add(BigDecimal.valueOf(0.20)));

        // Then
        verify(messagingTemplate, atLeast(2)).convertAndSend(eq("/topic/prices/currency"), captor.capture());
        PriceUpdateMessage lastMessage = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastMessage.getPreviousPrice()).isEqualTo(firstPrice);
    }
}
