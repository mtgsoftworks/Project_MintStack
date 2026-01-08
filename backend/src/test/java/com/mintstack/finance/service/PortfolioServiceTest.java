package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.AddPortfolioItemRequest;
import com.mintstack.finance.dto.request.CreatePortfolioRequest;
import com.mintstack.finance.dto.response.PortfolioResponse;
import com.mintstack.finance.dto.response.PortfolioItemResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Portfolio;
import com.mintstack.finance.entity.PortfolioItem;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.exception.BadRequestException;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PortfolioItemRepository;
import com.mintstack.finance.repository.PortfolioRepository;
import com.mintstack.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService Tests")
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
    private PortfolioItem testItem;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .keycloakId("test-keycloak-id")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        testPortfolio = Portfolio.builder()
                .id(1L)
                .name("Ana Portföy")
                .description("Test portföyü")
                .user(testUser)
                .isDefault(true)
                .build();

        testInstrument = Instrument.builder()
                .id(1L)
                .symbol("THYAO")
                .name("Türk Hava Yolları")
                .type(Instrument.InstrumentType.STOCK)
                .currentPrice(new BigDecimal("280.50"))
                .previousClose(new BigDecimal("275.00"))
                .isActive(true)
                .build();

        testItem = PortfolioItem.builder()
                .id(1L)
                .portfolio(testPortfolio)
                .instrument(testInstrument)
                .quantity(new BigDecimal("100"))
                .averageCost(new BigDecimal("250.00"))
                .build();

        testPortfolio.setItems(Arrays.asList(testItem));
    }

    @Nested
    @DisplayName("Get Portfolio Tests")
    class GetPortfolioTests {

        @Test
        @DisplayName("Should return user portfolios")
        void getPortfolios_shouldReturnUserPortfolios() {
            // Given
            when(userRepository.findByKeycloakId(anyString()))
                    .thenReturn(Optional.of(testUser));
            when(portfolioRepository.findByUserId(anyLong()))
                    .thenReturn(Arrays.asList(testPortfolio));

            // When
            List<PortfolioResponse> result = portfolioService.getPortfolios("test-keycloak-id");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Ana Portföy");
        }

        @Test
        @DisplayName("Should return empty list when no portfolios")
        void getPortfolios_whenNoPortfolios_shouldReturnEmptyList() {
            // Given
            when(userRepository.findByKeycloakId(anyString()))
                    .thenReturn(Optional.of(testUser));
            when(portfolioRepository.findByUserId(anyLong()))
                    .thenReturn(Collections.emptyList());

            // When
            List<PortfolioResponse> result = portfolioService.getPortfolios("test-keycloak-id");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return portfolio by id")
        void getPortfolio_shouldReturnPortfolio() {
            // Given
            when(userRepository.findByKeycloakId(anyString()))
                    .thenReturn(Optional.of(testUser));
            when(portfolioRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testPortfolio));

            // When
            PortfolioResponse result = portfolioService.getPortfolio("test-keycloak-id", 1L);

            // Then
            assertThat(result.getName()).isEqualTo("Ana Portföy");
            assertThat(result.getIsDefault()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when portfolio not found")
        void getPortfolio_whenNotFound_shouldThrowException() {
            // Given
            when(userRepository.findByKeycloakId(anyString()))
                    .thenReturn(Optional.of(testUser));
            when(portfolioRepository.findByIdAndUserId(anyLong(), anyLong()))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> portfolioService.getPortfolio("test-keycloak-id", 999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Create Portfolio Tests")
    class CreatePortfolioTests {

        @Test
        @DisplayName("Should create new portfolio")
        void createPortfolio_shouldCreate() {
            // Given
            CreatePortfolioRequest request = new CreatePortfolioRequest();
            request.setName("Yeni Portföy");
            request.setDescription("Test açıklama");

            when(userRepository.findByKeycloakId(anyString()))
                    .thenReturn(Optional.of(testUser));
            when(portfolioRepository.save(any(Portfolio.class)))
                    .thenAnswer(invocation -> {
                        Portfolio p = invocation.getArgument(0);
                        p.setId(2L);
                        return p;
                    });

            // When
            PortfolioResponse result = portfolioService.createPortfolio("test-keycloak-id", request);

            // Then
            assertThat(result.getName()).isEqualTo("Yeni Portföy");
            verify(portfolioRepository, times(1)).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void createPortfolio_whenUserNotFound_shouldThrowException() {
            // Given
            CreatePortfolioRequest request = new CreatePortfolioRequest();
            request.setName("Test");

            when(userRepository.findByKeycloakId(anyString()))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> portfolioService.createPortfolio("invalid-id", request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Portfolio Tests")
    class DeletePortfolioTests {

        @Test
        @DisplayName("Should delete portfolio")
        void deletePortfolio_shouldDelete() {
            // Given
            testPortfolio.setIsDefault(false);
            when(userRepository.findByKeycloakId(anyString()))
                    .thenReturn(Optional.of(testUser));
            when(portfolioRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testPortfolio));

            // When
            portfolioService.deletePortfolio("test-keycloak-id", 1L);

            // Then
            verify(portfolioRepository, times(1)).delete(testPortfolio);
        }

        @Test
        @DisplayName("Should throw exception when deleting default portfolio")
        void deletePortfolio_whenDefault_shouldThrowException() {
            // Given
            when(userRepository.findByKeycloakId(anyString()))
                    .thenReturn(Optional.of(testUser));
            when(portfolioRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testPortfolio));

            // When/Then
            assertThatThrownBy(() -> portfolioService.deletePortfolio("test-keycloak-id", 1L))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Portfolio Item Tests")
    class PortfolioItemTests {

        @Test
        @DisplayName("Should add item to portfolio")
        void addItem_shouldAddItem() {
            // Given
            AddPortfolioItemRequest request = new AddPortfolioItemRequest();
            request.setSymbol("GARAN");
            request.setQuantity(new BigDecimal("50"));
            request.setAverageCost(new BigDecimal("45.00"));

            Instrument garan = Instrument.builder()
                    .id(2L)
                    .symbol("GARAN")
                    .name("Garanti Bankası")
                    .currentPrice(new BigDecimal("50.00"))
                    .build();

            when(userRepository.findByKeycloakId(anyString()))
                    .thenReturn(Optional.of(testUser));
            when(portfolioRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testPortfolio));
            when(instrumentRepository.findBySymbol("GARAN"))
                    .thenReturn(Optional.of(garan));
            when(portfolioItemRepository.findByPortfolioIdAndInstrumentId(anyLong(), anyLong()))
                    .thenReturn(Optional.empty());
            when(portfolioItemRepository.save(any(PortfolioItem.class)))
                    .thenAnswer(invocation -> {
                        PortfolioItem item = invocation.getArgument(0);
                        item.setId(2L);
                        return item;
                    });

            // When
            PortfolioItemResponse result = portfolioService.addItem("test-keycloak-id", 1L, request);

            // Then
            assertThat(result.getSymbol()).isEqualTo("GARAN");
            verify(portfolioItemRepository, times(1)).save(any(PortfolioItem.class));
        }

        @Test
        @DisplayName("Should remove item from portfolio")
        void removeItem_shouldRemoveItem() {
            // Given
            when(userRepository.findByKeycloakId(anyString()))
                    .thenReturn(Optional.of(testUser));
            when(portfolioRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testPortfolio));
            when(portfolioItemRepository.findByIdAndPortfolioId(1L, 1L))
                    .thenReturn(Optional.of(testItem));

            // When
            portfolioService.removeItem("test-keycloak-id", 1L, 1L);

            // Then
            verify(portfolioItemRepository, times(1)).delete(testItem);
        }
    }

    @Nested
    @DisplayName("Portfolio Calculation Tests")
    class PortfolioCalculationTests {

        @Test
        @DisplayName("Should calculate total value correctly")
        void getTotalValue_shouldCalculateCorrectly() {
            // Given - testItem: 100 shares * 280.50 current price = 28,050
            when(userRepository.findByKeycloakId(anyString()))
                    .thenReturn(Optional.of(testUser));
            when(portfolioRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testPortfolio));

            // When
            PortfolioResponse result = portfolioService.getPortfolio("test-keycloak-id", 1L);

            // Then
            // Total Value = 100 * 280.50 = 28,050
            assertThat(result.getTotalValue()).isEqualByComparingTo(new BigDecimal("28050.00"));
        }

        @Test
        @DisplayName("Should calculate profit/loss correctly")
        void getProfitLoss_shouldCalculateCorrectly() {
            // Given - testItem: cost 100 * 250 = 25,000, value = 100 * 280.50 = 28,050
            // Profit = 28,050 - 25,000 = 3,050
            when(userRepository.findByKeycloakId(anyString()))
                    .thenReturn(Optional.of(testUser));
            when(portfolioRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testPortfolio));

            // When
            PortfolioResponse result = portfolioService.getPortfolio("test-keycloak-id", 1L);

            // Then
            assertThat(result.getProfitLoss()).isEqualByComparingTo(new BigDecimal("3050.00"));
        }
    }
}
