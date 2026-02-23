package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.UpdateProfileRequest;
import com.mintstack.finance.dto.response.UserResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.UserNotification;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.UserNotificationRepository;
import com.mintstack.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@io.micrometer.observation.annotation.Observed(name = "user.service", contextualName = "user-operations")
public class UserService {

    private final UserRepository userRepository;
    private final UserNotificationRepository notificationRepository;

    @Transactional
    public User getOrCreateUser(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        
        return userRepository.findByKeycloakId(keycloakId)
            .orElseGet(() -> createUserFromJwt(jwt));
    }

    private User createUserFromJwt(Jwt jwt) {
        log.info("Creating new user from JWT: {}", jwt.getSubject());
        
        User user = User.builder()
            .keycloakId(jwt.getSubject())
            .email(jwt.getClaimAsString("email"))
            .firstName(jwt.getClaimAsString("given_name"))
            .lastName(jwt.getClaimAsString("family_name"))
            .isActive(true)
            .build();
        
        return userRepository.save(user);
    }

    @Cacheable(value = "users", key = "#keycloakId")
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "keycloakId", keycloakId));
        
        return mapToResponse(user);
    }

    @CacheEvict(value = "users", key = "#keycloakId")
    @Transactional
    public UserResponse updateProfile(String keycloakId, UpdateProfileRequest request) {
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "keycloakId", keycloakId));
        
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
        if (request.getEmailNotifications() != null) {
            user.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getPushNotifications() != null) {
            user.setPushNotifications(request.getPushNotifications());
        }
        if (request.getPriceAlerts() != null) {
            user.setPriceAlerts(request.getPriceAlerts());
        }
        if (request.getPortfolioUpdates() != null) {
            user.setPortfolioUpdates(request.getPortfolioUpdates());
        }
        if (request.getCompactView() != null) {
            user.setCompactView(request.getCompactView());
        }
        
        User updatedUser = userRepository.save(user);
        log.info("User profile updated: {}", keycloakId);
        
        return mapToResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public User getUserByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "keycloakId", keycloakId));
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "id", id));
    }

    @CacheEvict(value = "users", key = "#user.keycloakId")
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", key = "#keycloakId")
    @Transactional
    public void markNotificationAsRead(String keycloakId, UUID notificationId) {
        User user = getUserByKeycloakId(keycloakId);
        notificationRepository.markAsRead(notificationId, user);
    }

    @CacheEvict(value = "users", key = "#keycloakId")
    @Transactional
    public int markAllNotificationsAsRead(String keycloakId) {
        User user = getUserByKeycloakId(keycloakId);
        return notificationRepository.markAllAsRead(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .fullName(user.getFullName())
            .isActive(user.getIsActive())
            .portfolioCount(user.getPortfolios().size())
            .createdAt(user.getCreatedAt())
            .phoneNumber(user.getPhoneNumber())
            .bio(user.getBio())
            .location(user.getLocation())
            .emailNotifications(user.getEmailNotifications())
            .pushNotifications(user.getPushNotifications())
            .priceAlerts(user.getPriceAlerts())
            .portfolioUpdates(user.getPortfolioUpdates())
            .compactView(user.getCompactView())
            .build();
    }
}
