package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.CreateWatchlistRequest;
import com.mintstack.finance.dto.response.WatchlistResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.Watchlist;
import com.mintstack.finance.entity.WatchlistItem;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.exception.BusinessException;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@io.micrometer.observation.annotation.Observed(name = "watchlist.service", contextualName = "watchlist-operations")
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserService userService;
    private final InstrumentRepository instrumentRepository;

    private static final int MAX_WATCHLISTS_PER_USER = 10;
    private static final int MAX_ITEMS_PER_WATCHLIST = 50;

    @Transactional(readOnly = true)
    public List<WatchlistResponse> getUserWatchlists(String keycloakId) {
        User user = userService.getUserByKeycloakId(keycloakId);
        return watchlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WatchlistResponse getWatchlist(String keycloakId, UUID watchlistId) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Watchlist watchlist = watchlistRepository.findByIdAndUserIdWithItems(watchlistId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist", "id", watchlistId));
        return mapToResponse(watchlist);
    }

    @Transactional
    public WatchlistResponse createWatchlist(String keycloakId, CreateWatchlistRequest request) {
        User user = userService.getUserByKeycloakId(keycloakId);

        // Check max watchlist limit
        long count = watchlistRepository.countByUserId(user.getId());
        if (count >= MAX_WATCHLISTS_PER_USER) {
            throw new BusinessException("Maksimum watchlist sayısına ulaştınız (" + MAX_WATCHLISTS_PER_USER + ")");
        }

        // Check name uniqueness
        if (watchlistRepository.existsByUserIdAndName(user.getId(), request.getName())) {
            throw new BusinessException("Bu isimde bir watchlist zaten mevcut");
        }

        Watchlist watchlist = Watchlist.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .isDefault(count == 0) // First watchlist is default
                .build();

        watchlist = watchlistRepository.save(watchlist);
        log.info("Created watchlist '{}' for user {}", request.getName(), keycloakId);
        return mapToResponse(watchlist);
    }

    @Transactional
    public WatchlistResponse updateWatchlist(String keycloakId, UUID watchlistId, CreateWatchlistRequest request) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Watchlist watchlist = watchlistRepository.findByIdAndUserId(watchlistId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist", "id", watchlistId));

        watchlist.setName(request.getName());
        watchlist.setDescription(request.getDescription());
        
        watchlist = watchlistRepository.save(watchlist);
        return mapToResponse(watchlist);
    }

    @Transactional
    public void deleteWatchlist(String keycloakId, UUID watchlistId) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Watchlist watchlist = watchlistRepository.findByIdAndUserId(watchlistId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist", "id", watchlistId));

        watchlistRepository.delete(watchlist);
        log.info("Deleted watchlist '{}' for user {}", watchlist.getName(), keycloakId);
    }

    @Transactional
    public WatchlistResponse addInstrument(String keycloakId, UUID watchlistId, String symbol) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Watchlist watchlist = watchlistRepository.findByIdAndUserIdWithItems(watchlistId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist", "id", watchlistId));

        // Check max items limit
        if (watchlist.getItems().size() >= MAX_ITEMS_PER_WATCHLIST) {
            throw new BusinessException("Watchlist'e maksimum " + MAX_ITEMS_PER_WATCHLIST + " enstrüman ekleyebilirsiniz");
        }

        String normalizedSymbol = normalizeSymbol(symbol);
        Instrument instrument = resolveInstrument(normalizedSymbol)
                .orElseThrow(() -> new ResourceNotFoundException("Enstrüman", "symbol", normalizedSymbol));

        // Check if already in watchlist
        boolean exists = watchlist.getItems().stream()
                .anyMatch(item -> item.getInstrument().getSymbol().equals(normalizedSymbol));
        if (exists) {
            throw new BusinessException("Bu enstrüman zaten watchlist'te mevcut");
        }

        watchlist.addItem(instrument);
        watchlist = watchlistRepository.save(watchlist);
        
        log.info("Added {} to watchlist {} for user {}", normalizedSymbol, watchlist.getName(), keycloakId);
        return mapToResponse(watchlist);
    }

    @Transactional
    public WatchlistResponse removeInstrument(String keycloakId, UUID watchlistId, String symbol) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Watchlist watchlist = watchlistRepository.findByIdAndUserIdWithItems(watchlistId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist", "id", watchlistId));

        String normalizedSymbol = normalizeSymbol(symbol);
        Instrument instrument = resolveInstrument(normalizedSymbol)
                .orElseThrow(() -> new ResourceNotFoundException("Enstrüman", "symbol", normalizedSymbol));

        watchlist.removeItem(instrument);
        watchlist = watchlistRepository.save(watchlist);
        
        log.info("Removed {} from watchlist {} for user {}", normalizedSymbol, watchlist.getName(), keycloakId);
        return mapToResponse(watchlist);
    }

    private String normalizeSymbol(String symbol) {
        return symbol != null ? symbol.trim().toUpperCase() : "";
    }

    private Optional<Instrument> resolveInstrument(String normalizedSymbol) {
        if (normalizedSymbol.isBlank()) {
            return Optional.empty();
        }

        List<String> candidates = buildSymbolCandidates(normalizedSymbol);
        for (String candidate : candidates) {
            Optional<Instrument> instrument = instrumentRepository.findBySymbol(candidate)
                .or(() -> instrumentRepository.findBySymbolAndIsSimulated(candidate, true));

            if (instrument.isPresent()) {
                return instrument;
            }
        }

        return Optional.empty();
    }

    private List<String> buildSymbolCandidates(String normalizedSymbol) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(normalizedSymbol);

        if (normalizedSymbol.endsWith(".IS")) {
            candidates.add(normalizedSymbol.substring(0, normalizedSymbol.length() - 3));
        } else {
            candidates.add(normalizedSymbol + ".IS");
        }

        return new ArrayList<>(candidates);
    }



    private WatchlistResponse mapToResponse(Watchlist watchlist) {
        return WatchlistResponse.builder()
                .id(watchlist.getId())
                .name(watchlist.getName())
                .description(watchlist.getDescription())
                .isDefault(watchlist.getIsDefault())
                .itemCount(watchlist.getItems() != null ? watchlist.getItems().size() : 0)
                .items(watchlist.getItems() != null ? 
                    watchlist.getItems().stream()
                        .map(this::mapItemToResponse)
                        .collect(Collectors.toList()) : null)
                .createdAt(watchlist.getCreatedAt())
                .build();
    }

    private WatchlistResponse.WatchlistItemResponse mapItemToResponse(WatchlistItem item) {
        Instrument inst = item.getInstrument();
        return WatchlistResponse.WatchlistItemResponse.builder()
                .id(item.getId())
                .symbol(inst.getSymbol())
                .name(inst.getName())
                .type(inst.getType().name())
                .currentPrice(inst.getCurrentPrice())
                .previousClose(inst.getPreviousClose())
                .addedAt(item.getAddedAt())
                .build();
    }
}

