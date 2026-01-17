package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.AddPortfolioItemRequest;
import com.mintstack.finance.dto.request.CreatePortfolioRequest;
import com.mintstack.finance.dto.response.PortfolioResponse;
import com.mintstack.finance.dto.response.PortfolioTransactionResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Portfolio;
import com.mintstack.finance.entity.PortfolioItem;
import com.mintstack.finance.entity.PortfolioTransaction;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PortfolioItemRepository;
import com.mintstack.finance.repository.PortfolioRepository;
import com.mintstack.finance.repository.PortfolioTransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private PortfolioTransactionRepository portfolioTransactionRepository;

    @Mock
    private UserService userService;

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
        when(userService.getUserByKeycloakId(keycloakId)).thenReturn(testUser);
        when(portfolioRepository.findByUserIdWithItems(testUser.getId()))
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
        when(userService.getUserByKeycloakId(keycloakId)).thenReturn(testUser);
        when(portfolioRepository.findByIdAndUserIdWithItems(portfolioId, testUser.getId()))
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
        when(userService.getUserByKeycloakId(keycloakId)).thenReturn(testUser);
        when(portfolioRepository.findByIdAndUserIdWithItems(portfolioId, testUser.getId()))
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

        when(userService.getUserByKeycloakId(keycloakId)).thenReturn(testUser);
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
        when(userService.getUserByKeycloakId(keycloakId)).thenReturn(testUser);
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

        when(userService.getUserByKeycloakId(keycloakId)).thenReturn(testUser);
        when(portfolioRepository.findByIdAndUserId(portfolioId, testUser.getId()))
            .thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.findByIdAndUserIdWithItems(portfolioId, testUser.getId()))
            .thenReturn(Optional.of(testPortfolio));
        when(instrumentRepository.findById(testInstrument.getId()))
            .thenReturn(Optional.of(testInstrument));
        when(portfolioItemRepository.save(any(PortfolioItem.class))).thenReturn(savedItem);
        when(portfolioTransactionRepository.save(any(PortfolioTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var result = portfolioService.addItem(keycloakId, portfolioId, request);

        // Then
        assertThat(result).isNotNull();
        verify(portfolioItemRepository).save(any(PortfolioItem.class));
        ArgumentCaptor<PortfolioTransaction> transactionCaptor = ArgumentCaptor.forClass(PortfolioTransaction.class);
        verify(portfolioTransactionRepository).save(transactionCaptor.capture());
        PortfolioTransaction transaction = transactionCaptor.getValue();
        assertThat(transaction.getTransactionType()).isEqualTo(PortfolioTransaction.TransactionType.BUY);
        assertThat(transaction.getQuantity()).isEqualByComparingTo(request.getQuantity());
        assertThat(transaction.getPrice()).isEqualByComparingTo(request.getPurchasePrice());
        assertThat(transaction.getInstrument()).isEqualTo(testInstrument);
        assertThat(transaction.getPortfolio()).isEqualTo(testPortfolio);
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

        when(userService.getUserByKeycloakId(keycloakId)).thenReturn(testUser);
        when(portfolioRepository.findByIdAndUserId(portfolioId, testUser.getId()))
            .thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.findByIdAndUserIdWithItems(portfolioId, testUser.getId()))
            .thenReturn(Optional.of(testPortfolio));
        when(portfolioItemRepository.findByIdAndPortfolioId(itemId, portfolioId))
            .thenReturn(Optional.of(item));
        when(portfolioTransactionRepository.save(any(PortfolioTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        portfolioService.removeItem(keycloakId, portfolioId, itemId);

        // Then
        verify(portfolioItemRepository).delete(item);
        ArgumentCaptor<PortfolioTransaction> transactionCaptor = ArgumentCaptor.forClass(PortfolioTransaction.class);
        verify(portfolioTransactionRepository).save(transactionCaptor.capture());
        PortfolioTransaction transaction = transactionCaptor.getValue();
        assertThat(transaction.getTransactionType()).isEqualTo(PortfolioTransaction.TransactionType.SELL);
        assertThat(transaction.getQuantity()).isEqualByComparingTo(item.getQuantity());
        assertThat(transaction.getPrice()).isEqualByComparingTo(testInstrument.getCurrentPrice());
        assertThat(transaction.getInstrument()).isEqualTo(testInstrument);
        assertThat(transaction.getPortfolio()).isEqualTo(testPortfolio);
    }

    @Test
    void getPortfolioTransactions_ShouldReturnTransactions() {
        // Given
        UUID portfolioId = testPortfolio.getId();
        Pageable pageable = PageRequest.of(0, 20);
        PortfolioTransaction transaction = PortfolioTransaction.builder()
            .portfolio(testPortfolio)
            .instrument(testInstrument)
            .transactionType(PortfolioTransaction.TransactionType.BUY)
            .quantity(BigDecimal.valueOf(2))
            .price(BigDecimal.valueOf(95))
            .transactionDate(LocalDate.now())
            .notes("Test işlem")
            .build();
        transaction.setId(UUID.randomUUID());
        transaction.setCreatedAt(LocalDateTime.now());

        Page<PortfolioTransaction> page = new PageImpl<>(List.of(transaction), pageable, 1);

        when(userService.getUserByKeycloakId(keycloakId)).thenReturn(testUser);
        when(portfolioRepository.findByIdAndUserId(portfolioId, testUser.getId()))
            .thenReturn(Optional.of(testPortfolio));
        when(portfolioTransactionRepository.findByPortfolioIdAndUserId(portfolioId, testUser.getId(), pageable))
            .thenReturn(page);

        // When
        Page<PortfolioTransactionResponse> result = portfolioService.getPortfolioTransactions(
            keycloakId,
            portfolioId,
            pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        PortfolioTransactionResponse response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo(transaction.getId());
        assertThat(response.getInstrumentSymbol()).isEqualTo(testInstrument.getSymbol());
        assertThat(response.getTransactionType()).isEqualTo(transaction.getTransactionType());
        assertThat(response.getTotal())
            .isEqualByComparingTo(transaction.getPrice().multiply(transaction.getQuantity()));
    }
}
