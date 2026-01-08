package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.UpdateProfileRequest;
import com.mintstack.finance.dto.response.UserResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .keycloakId("test-keycloak-id")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("+905551234567")
                .notificationsEnabled(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {

        @Test
        @DisplayName("Should return user by keycloak id")
        void getUser_shouldReturnUser() {
            // Given
            when(userRepository.findByKeycloakId("test-keycloak-id"))
                    .thenReturn(Optional.of(testUser));

            // When
            UserResponse result = userService.getUser("test-keycloak-id");

            // Then
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getFirstName()).isEqualTo("Test");
            assertThat(result.getLastName()).isEqualTo("User");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getUser_whenNotFound_shouldThrowException() {
            // Given
            when(userRepository.findByKeycloakId("invalid-id"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.getUser("invalid-id"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should return user profile with full details")
        void getUserProfile_shouldReturnFullProfile() {
            // Given
            when(userRepository.findByKeycloakId("test-keycloak-id"))
                    .thenReturn(Optional.of(testUser));

            // When
            UserResponse result = userService.getUserProfile("test-keycloak-id");

            // Then
            assertThat(result.getPhone()).isEqualTo("+905551234567");
            assertThat(result.getNotificationsEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Create/Update User Tests")
    class CreateUpdateUserTests {

        @Test
        @DisplayName("Should create new user if not exists")
        void getOrCreateUser_shouldCreateNewUser() {
            // Given
            when(userRepository.findByKeycloakId("new-keycloak-id"))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> {
                        User user = invocation.getArgument(0);
                        user.setId(2L);
                        return user;
                    });

            // When
            UserResponse result = userService.getOrCreateUser(
                    "new-keycloak-id",
                    "new@example.com",
                    "New",
                    "User"
            );

            // Then
            assertThat(result.getEmail()).isEqualTo("new@example.com");
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Should return existing user without creating")
        void getOrCreateUser_shouldReturnExistingUser() {
            // Given
            when(userRepository.findByKeycloakId("test-keycloak-id"))
                    .thenReturn(Optional.of(testUser));

            // When
            UserResponse result = userService.getOrCreateUser(
                    "test-keycloak-id",
                    "test@example.com",
                    "Test",
                    "User"
            );

            // Then
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should update user profile")
        void updateProfile_shouldUpdateUser() {
            // Given
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Updated");
            request.setLastName("Name");
            request.setPhone("+905559876543");
            request.setNotificationsEnabled(false);

            when(userRepository.findByKeycloakId("test-keycloak-id"))
                    .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse result = userService.updateProfile("test-keycloak-id", request);

            // Then
            assertThat(result.getFirstName()).isEqualTo("Updated");
            assertThat(result.getLastName()).isEqualTo("Name");
            assertThat(result.getPhone()).isEqualTo("+905559876543");
            assertThat(result.getNotificationsEnabled()).isFalse();
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent user")
        void updateProfile_whenUserNotFound_shouldThrowException() {
            // Given
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Test");

            when(userRepository.findByKeycloakId("invalid-id"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.updateProfile("invalid-id", request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("User Sync Tests")
    class UserSyncTests {

        @Test
        @DisplayName("Should sync user from Keycloak")
        void syncFromKeycloak_shouldUpdateUserInfo() {
            // Given
            when(userRepository.findByKeycloakId("test-keycloak-id"))
                    .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.syncFromKeycloak(
                    "test-keycloak-id",
                    "updated@example.com",
                    "Updated",
                    "UserName"
            );

            // Then
            verify(userRepository, times(1)).save(argThat(user ->
                    user.getEmail().equals("updated@example.com") &&
                    user.getFirstName().equals("Updated") &&
                    user.getLastName().equals("UserName")
            ));
        }

        @Test
        @DisplayName("Should create user if not exists during sync")
        void syncFromKeycloak_shouldCreateIfNotExists() {
            // Given
            when(userRepository.findByKeycloakId("new-keycloak-id"))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> {
                        User user = invocation.getArgument(0);
                        user.setId(3L);
                        return user;
                    });

            // When
            userService.syncFromKeycloak(
                    "new-keycloak-id",
                    "new@example.com",
                    "New",
                    "User"
            );

            // Then
            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("User Preferences Tests")
    class UserPreferencesTests {

        @Test
        @DisplayName("Should toggle notifications")
        void toggleNotifications_shouldToggle() {
            // Given
            when(userRepository.findByKeycloakId("test-keycloak-id"))
                    .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When - Initial state is true, should become false
            userService.toggleNotifications("test-keycloak-id");

            // Then
            verify(userRepository, times(1)).save(argThat(user ->
                    !user.getNotificationsEnabled()
            ));
        }

        @Test
        @DisplayName("Should update last login")
        void updateLastLogin_shouldUpdateTimestamp() {
            // Given
            when(userRepository.findByKeycloakId("test-keycloak-id"))
                    .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            LocalDateTime before = LocalDateTime.now();

            // When
            userService.updateLastLogin("test-keycloak-id");

            // Then
            verify(userRepository, times(1)).save(argThat(user ->
                    user.getLastLoginAt() != null &&
                    !user.getLastLoginAt().isBefore(before)
            ));
        }
    }
}
