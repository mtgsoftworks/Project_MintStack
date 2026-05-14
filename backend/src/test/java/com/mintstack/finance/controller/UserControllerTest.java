package com.mintstack.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.config.SecurityConfig;
import com.mintstack.finance.dto.request.UpdateProfileRequest;
import com.mintstack.finance.dto.response.UserResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.UserNotification;
import com.mintstack.finance.repository.UserNotificationRepository;
import com.mintstack.finance.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({CorsProperties.class, SecurityConfig.class})
@AutoConfigureDataJpa
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserNotificationRepository notificationRepository;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    private static final String TEST_KEYCLOAK_ID = "test-keycloak-id-123";

    @Test
    void getProfile_ShouldReturnUserProfile() throws Exception {
        // Given
        UserResponse userResponse = UserResponse.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .firstName("John")
            .lastName("Doe")
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();

        when(userService.getOrCreateUser(any())).thenReturn(null);
        when(userService.getUserProfile(TEST_KEYCLOAK_ID)).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.firstName").value("John"));
    }

    @Test
    void updateProfile_ShouldReturnUpdatedProfile() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");

        UserResponse updatedUser = UserResponse.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .firstName("Jane")
            .lastName("Smith")
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();

        when(userService.updateProfile(eq(TEST_KEYCLOAK_ID), any(UpdateProfileRequest.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/v1/users/me")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.firstName").value("Jane"))
            .andExpect(jsonPath("$.data.lastName").value("Smith"));
    }

    @Test
    void getProfile_ShouldReturnUnauthorized_WhenNoAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_ShouldReturnUnauthorized_WhenNoAuth() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Jane");

        // When & Then
        mockMvc.perform(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getNotifications_ShouldReturnNotifications() throws Exception {
        User user = User.builder()
            .keycloakId(TEST_KEYCLOAK_ID)
            .email("user@example.com")
            .firstName("John")
            .lastName("Doe")
            .build();

        UserNotification notification = UserNotification.builder()
            .user(user)
            .title("Alert Triggered")
            .message("THYAO reached target price")
            .type(UserNotification.NotificationType.ALERT)
            .isRead(false)
            .build();
        notification.setId(UUID.randomUUID());
        notification.setCreatedAt(LocalDateTime.now());

        when(userService.getOrCreateUser(any())).thenReturn(user);
        when(notificationRepository.findByUserOrderByCreatedAtDesc(eq(user), any()))
            .thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationRepository.countByUserAndIsRead(user, false)).thenReturn(1L);

        mockMvc.perform(get("/api/v1/users/me/notifications")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].title").value("Alert Triggered"))
            .andExpect(jsonPath("$.data[0].type").value("ALERT"))
            .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    void markNotificationRead_ShouldReturnSuccess() throws Exception {
        UUID notificationId = UUID.randomUUID();
        doNothing().when(userService).markNotificationAsRead(TEST_KEYCLOAK_ID, notificationId);

        mockMvc.perform(post("/api/v1/users/me/notifications/{id}/read", notificationId)
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void markAllNotificationsRead_ShouldReturnUpdatedCount() throws Exception {
        when(userService.markAllNotificationsAsRead(TEST_KEYCLOAK_ID)).thenReturn(3);

        mockMvc.perform(post("/api/v1/users/me/notifications/read-all")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value(containsString("3 bildirim okundu olarak")));
    }
}
