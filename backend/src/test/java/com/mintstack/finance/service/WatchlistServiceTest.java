package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.CreateWatchlistRequest;
import com.mintstack.finance.dto.response.WatchlistResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.Watchlist;
import com.mintstack.finance.exception.BusinessException;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.UserRepository;
import com.mintstack.finance.repository.WatchlistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @InjectMocks
    private WatchlistService watchlistService;

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId("test-keycloak-id");
        user.setEmail("test@example.com");
        return user;
    }

    private Watchlist createTestWatchlist(User user) {
        Watchlist watchlist = Watchlist.builder()
            .user(user)
            .name("My Watchlist")
            .description("Test watchlist")
            .isDefault(true)
            .items(new ArrayList<>())
            .build();
        watchlist.setId(UUID.randomUUID());
        return watchlist;
    }

    private Instrument createTestInstrument() {
        Instrument instrument = new Instrument();
        instrument.setId(UUID.randomUUID());
        instrument.setSymbol("THYAO");
        instrument.setName("Turkish Airlines");
        instrument.setType(Instrument.InstrumentType.STOCK);
        return instrument;
    }

    @Test
    void getUserWatchlists_ShouldReturnWatchlists() {
        // Given
        User user = createTestUser();
        Watchlist watchlist = createTestWatchlist(user);

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(watchlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(watchlist));

        // When
        List<WatchlistResponse> result = watchlistService.getUserWatchlists("test-keycloak-id");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("My Watchlist");
    }

    @Test
    void getWatchlist_ShouldReturnWatchlist() {
        // Given
        User user = createTestUser();
        Watchlist watchlist = createTestWatchlist(user);

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(watchlistRepository.findByIdAndUserIdWithItems(watchlist.getId(), user.getId()))
            .thenReturn(Optional.of(watchlist));

        // When
        WatchlistResponse result = watchlistService.getWatchlist("test-keycloak-id", watchlist.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("My Watchlist");
    }

    @Test
    void createWatchlist_ShouldCreateWatchlist() {
        // Given
        User user = createTestUser();
        CreateWatchlistRequest request = new CreateWatchlistRequest();
        request.setName("New Watchlist");
        request.setDescription("Description");

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(watchlistRepository.countByUserId(user.getId())).thenReturn(0L);
        when(watchlistRepository.existsByUserIdAndName(user.getId(), "New Watchlist")).thenReturn(false);
        when(watchlistRepository.save(any(Watchlist.class))).thenAnswer(i -> {
            Watchlist saved = i.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setItems(Collections.emptyList());
            return saved;
        });

        // When
        WatchlistResponse result = watchlistService.createWatchlist("test-keycloak-id", request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Watchlist");
        assertThat(result.getIsDefault()).isTrue(); // First watchlist is default
    }

    @Test
    void createWatchlist_ShouldThrowWhenMaxReached() {
        // Given
        User user = createTestUser();
        CreateWatchlistRequest request = new CreateWatchlistRequest();
        request.setName("New Watchlist");

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(watchlistRepository.countByUserId(user.getId())).thenReturn(10L);

        // When & Then
        assertThatThrownBy(() -> watchlistService.createWatchlist("test-keycloak-id", request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Maksimum");
    }

    @Test
    void createWatchlist_ShouldThrowWhenNameExists() {
        // Given
        User user = createTestUser();
        CreateWatchlistRequest request = new CreateWatchlistRequest();
        request.setName("Existing Watchlist");

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(watchlistRepository.countByUserId(user.getId())).thenReturn(1L);
        when(watchlistRepository.existsByUserIdAndName(user.getId(), "Existing Watchlist")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> watchlistService.createWatchlist("test-keycloak-id", request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("zaten mevcut");
    }

    @Test
    void updateWatchlist_ShouldUpdateWatchlist() {
        // Given
        User user = createTestUser();
        Watchlist watchlist = createTestWatchlist(user);
        CreateWatchlistRequest request = new CreateWatchlistRequest();
        request.setName("Updated Name");
        request.setDescription("Updated Description");

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(watchlistRepository.findByIdAndUserId(watchlist.getId(), user.getId()))
            .thenReturn(Optional.of(watchlist));
        when(watchlistRepository.save(any(Watchlist.class))).thenReturn(watchlist);

        // When
        WatchlistResponse result = watchlistService.updateWatchlist("test-keycloak-id", watchlist.getId(), request);

        // Then
        assertThat(watchlist.getName()).isEqualTo("Updated Name");
        verify(watchlistRepository).save(watchlist);
    }

    @Test
    void deleteWatchlist_ShouldDeleteWatchlist() {
        // Given
        User user = createTestUser();
        Watchlist watchlist = createTestWatchlist(user);

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(watchlistRepository.findByIdAndUserId(watchlist.getId(), user.getId()))
            .thenReturn(Optional.of(watchlist));

        // When
        watchlistService.deleteWatchlist("test-keycloak-id", watchlist.getId());

        // Then
        verify(watchlistRepository).delete(watchlist);
    }

    @Test
    void addInstrument_ShouldAddInstrument() {
        // Given
        User user = createTestUser();
        Watchlist watchlist = createTestWatchlist(user);
        Instrument instrument = createTestInstrument();

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(watchlistRepository.findByIdAndUserIdWithItems(watchlist.getId(), user.getId()))
            .thenReturn(Optional.of(watchlist));
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(instrument));
        when(watchlistRepository.save(any(Watchlist.class))).thenReturn(watchlist);

        // When
        WatchlistResponse result = watchlistService.addInstrument("test-keycloak-id", watchlist.getId(), "THYAO");

        // Then
        verify(watchlistRepository).save(watchlist);
    }

    @Test
    void addInstrument_ShouldThrowWhenInstrumentNotFound() {
        // Given
        User user = createTestUser();
        Watchlist watchlist = createTestWatchlist(user);

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(watchlistRepository.findByIdAndUserIdWithItems(watchlist.getId(), user.getId()))
            .thenReturn(Optional.of(watchlist));
        when(instrumentRepository.findBySymbol("UNKNOWN")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> watchlistService.addInstrument("test-keycloak-id", watchlist.getId(), "UNKNOWN"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeInstrument_ShouldRemoveInstrument() {
        // Given
        User user = createTestUser();
        Watchlist watchlist = createTestWatchlist(user);
        Instrument instrument = createTestInstrument();

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(watchlistRepository.findByIdAndUserIdWithItems(watchlist.getId(), user.getId()))
            .thenReturn(Optional.of(watchlist));
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(instrument));
        when(watchlistRepository.save(any(Watchlist.class))).thenReturn(watchlist);

        // When
        WatchlistResponse result = watchlistService.removeInstrument("test-keycloak-id", watchlist.getId(), "THYAO");

        // Then
        verify(watchlistRepository).save(watchlist);
    }

    @Test
    void getUserWatchlists_ShouldThrowWhenUserNotFound() {
        // Given
        when(userRepository.findByKeycloakId("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> watchlistService.getUserWatchlists("unknown"))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
