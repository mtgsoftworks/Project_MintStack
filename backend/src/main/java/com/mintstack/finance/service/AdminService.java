package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.AdminDashboardResponse;
import com.mintstack.finance.dto.response.UserAdminResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final InstrumentRepository instrumentRepository;
    private final NewsRepository newsRepository;
    private final CurrencyRateRepository currencyRateRepository;
    private final PriceAlertRepository alertRepository;
    private final WatchlistRepository watchlistRepository;

    /**
     * Get admin dashboard statistics
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardStats() {
        return AdminDashboardResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByIsActiveTrue())
                .totalPortfolios(portfolioRepository.count())
                .totalInstruments(instrumentRepository.count())
                .activeInstruments(instrumentRepository.countByIsActiveTrue())
                .totalNews(newsRepository.count())
                .totalCurrencyRates(currencyRateRepository.count())
                .activeAlerts(alertRepository.findActiveAlerts().size())
                .totalWatchlists(watchlistRepository.count())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * Get all users with pagination
     */
    @Transactional(readOnly = true)
    public Page<UserAdminResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserAdminResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "id", userId));
        return mapToUserResponse(user);
    }

    /**
     * Activate user
     */
    @Transactional
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "id", userId));
        user.setIsActive(true);
        userRepository.save(user);
        log.info("User {} activated by admin", userId);
    }

    /**
     * Deactivate user
     */
    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "id", userId));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User {} deactivated by admin", userId);
    }

    /**
     * Search users
     */
    @Transactional(readOnly = true)
    public Page<UserAdminResponse> searchUsers(String query, Pageable pageable) {
        return userRepository.searchByEmailOrName(query, pageable)
                .map(this::mapToUserResponse);
    }

    private UserAdminResponse mapToUserResponse(User user) {
        return UserAdminResponse.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .username(user.getEmail()) // Username yok, email kullanıyoruz
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.getIsActive())
                .portfolioCount(user.getPortfolios() != null ? user.getPortfolios().size() : 0)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(null) // Last login takibi henüz yok
                .build();
    }
}
