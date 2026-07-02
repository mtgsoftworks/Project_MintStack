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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        Page<User> users = userRepository.findAll(pageable);
        return mapUserPage(users);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserAdminResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "id", userId));
        return mapToUserResponse(user, Math.toIntExact(portfolioRepository.countByUserId(userId)));
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
        return mapUserPage(userRepository.searchByEmailOrName(query, pageable));
    }

    private Page<UserAdminResponse> mapUserPage(Page<User> users) {
        List<UUID> userIds = users.getContent().stream().map(User::getId).toList();
        List<PortfolioRepository.UserPortfolioCount> counts = userIds.isEmpty()
                ? List.of()
                : portfolioRepository.countByUserIds(userIds);
        Map<UUID, Integer> countByUser = counts == null
                ? Map.of()
                : counts.stream().collect(Collectors.toMap(
                        PortfolioRepository.UserPortfolioCount::getUserId,
                        count -> Math.toIntExact(count.getPortfolioCount())
                ));
        return users.map(user -> mapToUserResponse(user, countByUser.getOrDefault(user.getId(), 0)));
    }

    private UserAdminResponse mapToUserResponse(User user, int portfolioCount) {
        return UserAdminResponse.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .username(user.getEmail()) // Username yok, email kullanıyoruz
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.getIsActive())
                .portfolioCount(portfolioCount)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(null) // Last login takibi henüz yok
                .build();
    }
}
