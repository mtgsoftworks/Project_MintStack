package com.mintstack.finance.controller;

import com.mintstack.finance.dto.request.UpdatePreferencesRequest;
import com.mintstack.finance.dto.request.UpdateProfileRequest;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.NotificationResponse;
import com.mintstack.finance.dto.response.PaginationInfo;
import com.mintstack.finance.dto.response.UserPreferencesResponse;
import com.mintstack.finance.dto.response.UserResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.UserNotification;
import com.mintstack.finance.repository.UserNotificationRepository;
import com.mintstack.finance.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "Kullanıcı yönetimi API'leri")
@SecurityRequirement(name = "bearer")
public class UserController {

    private final UserService userService;
    private final UserNotificationRepository notificationRepository;

    // ==================== PROFILE ====================

    @GetMapping({"/me", "/profile"})
    @Operation(summary = "Kullanıcı profilini getir")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal Jwt jwt) {
        
        userService.getOrCreateUser(jwt);
        UserResponse profile = userService.getUserProfile(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping({"/me", "/profile"})
    @Operation(summary = "Kullanıcı profilini güncelle")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        UserResponse profile = userService.updateProfile(jwt.getSubject(), request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profil başarıyla güncellendi"));
    }

    // ==================== PREFERENCES ====================

    @GetMapping("/me/preferences")
    @Operation(summary = "Kullanıcı tercihlerini getir")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> getPreferences(
            @AuthenticationPrincipal Jwt jwt) {
        
        User user = userService.getOrCreateUser(jwt);
        UserPreferencesResponse preferences = UserPreferencesResponse.builder()
                .emailNotifications(user.getEmailNotifications())
                .pushNotifications(user.getPushNotifications())
                .priceAlerts(user.getPriceAlerts())
                .portfolioUpdates(user.getPortfolioUpdates())
                .compactView(user.getCompactView())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    @PutMapping("/me/preferences")
    @Operation(summary = "Kullanıcı tercihlerini güncelle")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        
        User user = userService.getOrCreateUser(jwt);
        
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
        
        userService.saveUser(user);
        
        UserPreferencesResponse preferences = UserPreferencesResponse.builder()
                .emailNotifications(user.getEmailNotifications())
                .pushNotifications(user.getPushNotifications())
                .priceAlerts(user.getPriceAlerts())
                .portfolioUpdates(user.getPortfolioUpdates())
                .compactView(user.getCompactView())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(preferences, "Tercihler güncellendi"));
    }

    // ==================== NOTIFICATIONS ====================

    @GetMapping("/me/notifications")
    @Operation(summary = "Kullanıcı bildirimlerini getir")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        
        User user = userService.getOrCreateUser(jwt);
        Page<UserNotification> notifications = notificationRepository
                .findByUserOrderByCreatedAtDesc(user, pageable);
        
        List<NotificationResponse> response = notifications.getContent().stream()
                .map(NotificationResponse::from)
                .toList();
        
        long unreadCount = notificationRepository.countByUserAndIsRead(user, false);
        
        return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder()
                .success(true)
                .data(response)
                .pagination(PaginationInfo.from(notifications))
                .message("Okunmamış: " + unreadCount)
                .build());
    }

    @PostMapping("/me/notifications/{id}/read")
    @Operation(summary = "Bildirimi okundu olarak işaretle")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        
        User user = userService.getOrCreateUser(jwt);
        notificationRepository.markAsRead(id, user);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Bildirim okundu olarak işaretlendi"));
    }

    @PostMapping("/me/notifications/read-all")
    @Operation(summary = "Tüm bildirimleri okundu olarak işaretle")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsRead(
            @AuthenticationPrincipal Jwt jwt) {
        
        User user = userService.getOrCreateUser(jwt);
        int count = notificationRepository.markAllAsRead(user);
        
        return ResponseEntity.ok(ApiResponse.success(null, count + " bildirim okundu olarak işaretlendi"));
    }
}

