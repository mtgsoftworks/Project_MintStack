package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.AddPortfolioItemRequest;
import com.mintstack.finance.dto.request.CreatePortfolioRequest;
import com.mintstack.finance.dto.response.PortfolioResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Portfolio;
import com.mintstack.finance.entity.PortfolioItem;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PortfolioItemRepository;
import com.mintstack.finance.repository.PortfolioRepository;
import com.mintstack.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioItemRepository portfolioItemRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private User testUser;
    private Portfolio testPortfolio;
    private Instrument testInstrument;
    private String keycloakId;

    @BeforeEach
    void setUp() {
        keycloakId = "test-keycloak-id";
        
        testUser = User.builder()
            .keycloakId(keycloakId)
            .email("test@mintstack.local")
            .firstName("Test")
            .lastName("User")
            .isActive(true)
            .portfolios(new ArrayList<>())
            .build();
        testUser.setId(UUID.randomUUID());

        testPortfolio = Portfolio.builder()
            .name("Test Portfolio")
            .description("Test Description")
            .user(testUser)
            .isDefault(true)
            .items(new ArrayList<>())
            .build();
        testPortfolio.setId(UUID.randomUUID());

        testInstrument = Instrument.builder()
            .symbol("THYAO")
            .name("Türk Hava Yolları")
            .type(Instrument.InstrumentType.STOCK)
            .exchange("BIST")
            .currentPrice(BigDecimal.valueOf(100))
            .isActive(true)
            .build();
        testInstrument.setId(UUID.randomUUID());
    }

    @Test
    void getUserPortfolios_ShouldReturnPortfolios() {
        // Given
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(testUser));
        when(portfolioRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
            .thenReturn(List.of(testPortfolio));

        // When
        List<PortfolioResponse> result = portfolioService.getUserPortfolios(keycloakId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Portfolio");
    }

    @Test
    void getPortfolio_ShouldReturnPortfolio_WhenExists() {
        // Given
        UUID portfolioId = testPortfolio.getId();
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(testUser));
        when(portfolioRepository.findByIdAndUserId(portfolioId, testUser.getId()))
            .thenReturn(Optional.of(testPortfolio));

        // When
        PortfolioResponse result = portfolioService.getPortfolio(keycloakId, portfolioId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Portfolio");
    }

    @Test
    void getPortfolio_ShouldThrowException_WhenNotFound() {
        // Given
        UUID portfolioId = UUID.randomUUID();
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(testUser));
        when(portfolioRepository.findByIdAndUserId(portfolioId, testUser.getId()))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> portfolioService.getPortfolio(keycloakId, portfolioId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createPortfolio_ShouldCreateNewPortfolio() {
        // Given
        CreatePortfolioRequest request = CreatePortfolioRequest.builder()
            .name("New Portfolio")
            .description("New Description")
            .build();

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(testUser));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

        // When
        PortfolioResponse result = portfolioService.createPortfolio(keycloakId, request);

        // Then
        assertThat(result).isNotNull();
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    void deletePortfolio_ShouldDelete_WhenExists() {
        // Given
        UUID portfolioId = testPortfolio.getId();
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(testUser));
        when(portfolioRepository.findByIdAndUserId(portfolioId, testUser.getId()))
            .thenReturn(Optional.of(testPortfolio));

        // When
        portfolioService.deletePortfolio(keycloakId, portfolioId);

        // Then
        verify(portfolioRepository).delete(testPortfolio);
    }

    @Test
    void addItem_ShouldAddItemToPortfolio() {
        // Given
        UUID portfolioId = testPortfolio.getId();
        AddPortfolioItemRequest request = AddPortfolioItemRequest.builder()
            .instrumentId(testInstrument.getId())
            .quantity(BigDecimal.valueOf(10))
            .purchasePrice(BigDecimal.valueOf(95))
            .purchaseDate(LocalDate.now())
            .build();

        PortfolioItem savedItem = PortfolioItem.builder()
            .portfolio(testPortfolio)
            .instrument(testInstrument)
            .quantity(request.getQuantity())
            .purchasePrice(request.getPurchasePrice())
            .purchaseDate(request.getPurchaseDate())
            .build();
        savedItem.setId(UUID.randomUUID());

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(testUser));
        when(portfolioRepository.findByIdAndUserId(portfolioId, testUser.getId()))
            .thenReturn(Optional.of(testPortfolio));
        when(instrumentRepository.findById(testInstrument.getId()))
            .thenReturn(Optional.of(testInstrument));
        when(portfolioItemRepository.save(any(PortfolioItem.class))).thenReturn(savedItem);

        // When
        var result = portfolioService.addItem(keycloakId, portfolioId, request);

        // Then
        assertThat(result).isNotNull();
        verify(portfolioItemRepository).save(any(PortfolioItem.class));
    }

    @Test
    void removeItem_ShouldRemoveItem_WhenExists() {
        // Given
        UUID portfolioId = testPortfolio.getId();
        UUID itemId = UUID.randomUUID();

        PortfolioItem item = PortfolioItem.builder()
            .portfolio(testPortfolio)
            .instrument(testInstrument)
            .quantity(BigDecimal.TEN)
            .purchasePrice(BigDecimal.valueOf(100))
            .purchaseDate(LocalDate.now())
            .build();
        item.setId(itemId);

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(testUser));
        when(portfolioRepository.findByIdAndUserId(portfolioId, testUser.getId()))
            .thenReturn(Optional.of(testPortfolio));
        when(portfolioItemRepository.findByIdAndPortfolioId(itemId, portfolioId))
            .thenReturn(Optional.of(item));

        // When
        portfolioService.removeItem(keycloakId, portfolioId, itemId);

        // Then
        verify(portfolioItemRepository).delete(item);
    }
}
