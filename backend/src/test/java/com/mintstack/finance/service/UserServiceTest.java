package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.UpdateProfileRequest;
import com.mintstack.finance.dto.response.UserResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
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
    }

    @Test
    void getUserProfile_ShouldReturnUser_WhenUserExists() {
        // Given
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(testUser));

        // When
        UserResponse result = userService.getUserProfile(keycloakId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@mintstack.local");
        assertThat(result.getFirstName()).isEqualTo("Test");
        assertThat(result.getLastName()).isEqualTo("User");
    }

    @Test
    void getUserProfile_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserProfile(keycloakId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_ShouldUpdateUserFields() {
        // Given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
            .firstName("Updated")
            .lastName("Name")
            .build();

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse result = userService.updateProfile(keycloakId, request);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getOrCreateUser_ShouldReturnExistingUser() {
        // Given
        Jwt jwt = createMockJwt();
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getOrCreateUser(jwt);

        // Then
        assertThat(result).isEqualTo(testUser);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getOrCreateUser_ShouldCreateNewUser_WhenNotExists() {
        // Given
        Jwt jwt = createMockJwt();
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.getOrCreateUser(jwt);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUserByKeycloakId_ShouldReturnUser() {
        // Given
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserByKeycloakId(keycloakId);

        // Then
        assertThat(result).isEqualTo(testUser);
    }

    @Test
    void getUserById_ShouldReturnUser() {
        // Given
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserById(userId);

        // Then
        assertThat(result).isEqualTo(testUser);
    }

    private Jwt createMockJwt() {
        return new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "RS256"),
            Map.of(
                "sub", keycloakId,
                "email", "test@mintstack.local",
                "given_name", "Test",
                "family_name", "User"
            )
        );
    }
}
