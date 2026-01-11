package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.AdminDashboardResponse;
import com.mintstack.finance.dto.response.UserAdminResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @Mock
    private PriceAlertRepository alertRepository;

    @Mock
    private WatchlistRepository watchlistRepository;

    @InjectMocks
    private AdminService adminService;

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId("test-keycloak-id");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setIsActive(true);
        user.setPortfolios(Collections.emptyList());
        return user;
    }

    @Test
    void getDashboardStats_ShouldReturnStats() {
        // Given
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByIsActiveTrue()).thenReturn(85L);
        when(portfolioRepository.count()).thenReturn(50L);
        when(instrumentRepository.count()).thenReturn(200L);
        when(instrumentRepository.countByIsActiveTrue()).thenReturn(180L);
        when(newsRepository.count()).thenReturn(500L);
        when(currencyRateRepository.count()).thenReturn(20L);
        when(alertRepository.findActiveAlerts()).thenReturn(Collections.emptyList());
        when(watchlistRepository.count()).thenReturn(75L);

        // When
        AdminDashboardResponse result = adminService.getDashboardStats();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalUsers()).isEqualTo(100L);
        assertThat(result.getActiveUsers()).isEqualTo(85L);
        assertThat(result.getTotalPortfolios()).isEqualTo(50L);
        assertThat(result.getTotalInstruments()).isEqualTo(200L);
    }

    @Test
    void getAllUsers_ShouldReturnPaginatedUsers() {
        // Given
        User user = createTestUser();
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // When
        Page<UserAdminResponse> result = adminService.getAllUsers(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getUserById_ShouldReturnUser() {
        // Given
        User user = createTestUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // When
        UserAdminResponse result = adminService.getUserById(user.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getUserById_ShouldThrowWhenNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminService.getUserById(userId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activateUser_ShouldActivateUser() {
        // Given
        User user = createTestUser();
        user.setIsActive(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        adminService.activateUser(user.getId());

        // Then
        verify(userRepository).save(user);
        assertThat(user.getIsActive()).isTrue();
    }

    @Test
    void deactivateUser_ShouldDeactivateUser() {
        // Given
        User user = createTestUser();
        user.setIsActive(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        adminService.deactivateUser(user.getId());

        // Then
        verify(userRepository).save(user);
        assertThat(user.getIsActive()).isFalse();
    }

    @Test
    void searchUsers_ShouldReturnMatchingUsers() {
        // Given
        User user = createTestUser();
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        
        when(userRepository.searchByEmailOrName("test", pageable)).thenReturn(userPage);

        // When
        Page<UserAdminResponse> result = adminService.searchUsers("test", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }
}
