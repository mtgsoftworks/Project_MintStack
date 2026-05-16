package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.CreateWatchlistRequest;
import com.mintstack.finance.dto.request.ReorderWatchlistItemsRequest;
import com.mintstack.finance.dto.request.UpdateWatchlistItemRequest;
import com.mintstack.finance.dto.response.WatchlistResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.Watchlist;
import com.mintstack.finance.entity.WatchlistItem;
import com.mintstack.finance.exception.BusinessException;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@io.micrometer.observation.annotation.Observed(name = "watchlist.service", contextualName = "watchlist-operations")
public class WatchlistService {

    private static final int MAX_WATCHLISTS_PER_USER = 10;
    private static final int MAX_ITEMS_PER_WATCHLIST = 50;
    private static final String DEFAULT_WATCHLIST_NAME = "Favoriler";
    private static final Set<String> ALLOWED_COLUMN_PREFERENCES = Set.of(
        "SYMBOL", "NAME", "TYPE", "PRICE", "CHANGE", "ADDED_AT", "NOTES"
    );
    private static final List<String> DEFAULT_COLUMN_PREFERENCES = List.of(
        "SYMBOL", "NAME", "TYPE", "PRICE", "CHANGE", "NOTES"
    );

    private final WatchlistRepository watchlistRepository;
    private final UserService userService;
    private final InstrumentRepository instrumentRepository;

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
        Watchlist watchlist = loadWatchlistWithItems(user, watchlistId);
        return mapToResponse(watchlist);
    }

    @Transactional
    public WatchlistResponse createWatchlist(String keycloakId, CreateWatchlistRequest request) {
        User user = userService.getUserByKeycloakId(keycloakId);

        long count = watchlistRepository.countByUserId(user.getId());
        if (count >= MAX_WATCHLISTS_PER_USER) {
            throw new BusinessException("Maksimum watchlist sayisina ulastiniz (" + MAX_WATCHLISTS_PER_USER + ")");
        }

        String normalizedName = normalizeRequiredName(request.getName());
        if (watchlistRepository.existsByUserIdAndName(user.getId(), normalizedName)) {
            throw new BusinessException("Bu isimde bir watchlist zaten mevcut");
        }

        Watchlist watchlist = Watchlist.builder()
            .user(user)
            .name(normalizedName)
            .description(normalizeNullableText(request.getDescription()))
            .tag(normalizeNullableText(request.getTag()))
            .notes(normalizeNullableText(request.getNotes()))
            .columnPrefs(normalizeColumnPreferences(request.getColumnPreferences()))
            .isDefault(count == 0)
            .build();

        watchlist = watchlistRepository.save(watchlist);
        log.info("Created watchlist '{}' for user {}", watchlist.getName(), keycloakId);
        return mapToResponse(watchlist);
    }

    @Transactional
    public WatchlistResponse updateWatchlist(String keycloakId, UUID watchlistId, CreateWatchlistRequest request) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Watchlist watchlist = watchlistRepository.findByIdAndUserId(watchlistId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Watchlist", "id", watchlistId));

        String normalizedName = normalizeRequiredName(request.getName());
        if (!normalizedName.equals(watchlist.getName())
            && watchlistRepository.existsByUserIdAndName(user.getId(), normalizedName)) {
            throw new BusinessException("Bu isimde bir watchlist zaten mevcut");
        }

        watchlist.setName(normalizedName);
        watchlist.setDescription(normalizeNullableText(request.getDescription()));
        watchlist.setTag(normalizeNullableText(request.getTag()));
        watchlist.setNotes(normalizeNullableText(request.getNotes()));
        watchlist.setColumnPrefs(normalizeColumnPreferences(request.getColumnPreferences()));

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
        Watchlist watchlist = loadWatchlistWithItems(user, watchlistId);
        return addInstrumentToWatchlist(user, watchlist, symbol, keycloakId);
    }

    @Transactional
    public WatchlistResponse addInstrumentToDefaultWatchlist(String keycloakId, String symbol) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Watchlist watchlist = resolveOrCreateDefaultWatchlist(user);
        return addInstrumentToWatchlist(user, watchlist, symbol, keycloakId);
    }

    @Transactional
    public WatchlistResponse removeInstrument(String keycloakId, UUID watchlistId, String symbol) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Watchlist watchlist = loadWatchlistWithItems(user, watchlistId);

        String normalizedSymbol = normalizeSymbol(symbol);
        Instrument instrument = resolveInstrument(normalizedSymbol)
            .orElseThrow(() -> new ResourceNotFoundException("Enstruman", "symbol", normalizedSymbol));

        watchlist.removeItem(instrument);
        watchlist = watchlistRepository.save(watchlist);

        log.info("Removed {} from watchlist {} for user {}", normalizedSymbol, watchlist.getName(), keycloakId);
        return mapToResponse(watchlist);
    }

    @Transactional
    public WatchlistResponse reorderItems(String keycloakId, UUID watchlistId, ReorderWatchlistItemsRequest request) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Watchlist watchlist = loadWatchlistWithItems(user, watchlistId);

        List<WatchlistItem> items = watchlist.getItems() == null ? List.of() : watchlist.getItems();
        List<UUID> requestedOrder = request.getItemIds();

        if (items.isEmpty()) {
            throw new BusinessException("Bu watchlist'te siralanacak enstruman yok");
        }

        if (requestedOrder == null || requestedOrder.size() != items.size()) {
            throw new BusinessException("Siralama listesi item sayisi ile uyusmuyor");
        }

        Map<UUID, WatchlistItem> itemById = items.stream()
            .collect(Collectors.toMap(WatchlistItem::getId, Function.identity()));

        if (!itemById.keySet().equals(Set.copyOf(requestedOrder))) {
            throw new BusinessException("Siralama listesi gecersiz item id iceriyor");
        }

        for (int index = 0; index < requestedOrder.size(); index++) {
            WatchlistItem item = itemById.get(requestedOrder.get(index));
            item.setDisplayOrder(index + 1);
        }

        Watchlist saved = watchlistRepository.save(watchlist);
        return mapToResponse(saved);
    }

    @Transactional
    public WatchlistResponse updateItem(String keycloakId, UUID watchlistId, UUID itemId, UpdateWatchlistItemRequest request) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Watchlist watchlist = loadWatchlistWithItems(user, watchlistId);

        WatchlistItem item = watchlist.getItems().stream()
            .filter(candidate -> candidate.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("WatchlistItem", "id", itemId));

        item.setNotes(normalizeNullableText(request.getNotes()));

        Watchlist saved = watchlistRepository.save(watchlist);
        return mapToResponse(saved);
    }

    private Watchlist loadWatchlistWithItems(User user, UUID watchlistId) {
        return watchlistRepository.findByIdAndUserIdWithItems(watchlistId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Watchlist", "id", watchlistId));
    }

    private Watchlist resolveOrCreateDefaultWatchlist(User user) {
        Optional<Watchlist> existingDefault = watchlistRepository.findByUserIdAndIsDefaultTrue(user.getId());
        if (existingDefault.isPresent()) {
            return watchlistRepository.findByIdAndUserIdWithItems(existingDefault.get().getId(), user.getId())
                .orElse(existingDefault.get());
        }

        List<Watchlist> existing = watchlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (existing.isEmpty()) {
            Watchlist created = Watchlist.builder()
                .user(user)
                .name(DEFAULT_WATCHLIST_NAME)
                .description("Varsayilan izleme listesi")
                .isDefault(true)
                .columnPrefs(String.join(",", DEFAULT_COLUMN_PREFERENCES))
                .build();
            created = watchlistRepository.save(created);
            return watchlistRepository.findByIdAndUserIdWithItems(created.getId(), user.getId()).orElse(created);
        }

        Watchlist fallback = existing.get(0);
        fallback.setIsDefault(true);
        fallback = watchlistRepository.save(fallback);
        return watchlistRepository.findByIdAndUserIdWithItems(fallback.getId(), user.getId()).orElse(fallback);
    }

    private WatchlistResponse addInstrumentToWatchlist(User user, Watchlist watchlist, String symbol, String keycloakId) {
        if (watchlist.getItems().size() >= MAX_ITEMS_PER_WATCHLIST) {
            throw new BusinessException("Watchlist'e maksimum " + MAX_ITEMS_PER_WATCHLIST + " enstruman ekleyebilirsiniz");
        }

        String normalizedSymbol = normalizeSymbol(symbol);
        Instrument instrument = resolveInstrument(normalizedSymbol)
            .orElseThrow(() -> new ResourceNotFoundException("Enstruman", "symbol", normalizedSymbol));

        boolean exists = watchlist.getItems().stream()
            .anyMatch(item -> item.getInstrument().getId().equals(instrument.getId()));
        if (exists) {
            throw new BusinessException("Bu enstruman zaten watchlist'te mevcut");
        }

        int nextDisplayOrder = watchlist.getItems().stream()
            .map(WatchlistItem::getDisplayOrder)
            .filter(order -> order != null && order > 0)
            .max(Integer::compareTo)
            .orElse(0) + 1;

        watchlist.addItem(instrument, nextDisplayOrder);
        watchlist = watchlistRepository.save(watchlist);

        log.info("Added {} to watchlist {} for user {}", normalizedSymbol, watchlist.getName(), keycloakId);
        return mapToResponse(watchlist);
    }

    private String normalizeRequiredName(String name) {
        String normalized = normalizeNullableText(name);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("Watchlist adi bos olamaz");
        }
        return normalized;
    }

    private String normalizeNullableText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeColumnPreferences(List<String> columnPreferences) {
        if (columnPreferences == null || columnPreferences.isEmpty()) {
            return null;
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : columnPreferences) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String candidate = value.trim().toUpperCase(Locale.ROOT);
            if (ALLOWED_COLUMN_PREFERENCES.contains(candidate)) {
                normalized.add(candidate);
            }
        }

        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    private String normalizeSymbol(String symbol) {
        return symbol != null ? symbol.trim().toUpperCase(Locale.ROOT) : "";
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

    private List<String> parseColumnPreferences(String rawColumnPreferences) {
        if (!StringUtils.hasText(rawColumnPreferences)) {
            return DEFAULT_COLUMN_PREFERENCES;
        }

        List<String> parsed = List.of(rawColumnPreferences.split(","))
            .stream()
            .map(value -> value.trim().toUpperCase(Locale.ROOT))
            .filter(ALLOWED_COLUMN_PREFERENCES::contains)
            .distinct()
            .collect(Collectors.toList());

        return parsed.isEmpty() ? DEFAULT_COLUMN_PREFERENCES : parsed;
    }

    private WatchlistResponse mapToResponse(Watchlist watchlist) {
        List<WatchlistItem> sortedItems = watchlist.getItems() == null
            ? List.of()
            : watchlist.getItems().stream()
                .sorted(
                    Comparator.comparing(
                        WatchlistItem::getDisplayOrder,
                        Comparator.nullsLast(Integer::compareTo)
                    ).thenComparing(
                        WatchlistItem::getAddedAt,
                        Comparator.nullsLast(LocalDateTime::compareTo)
                    )
                )
                .collect(Collectors.toList());

        return WatchlistResponse.builder()
            .id(watchlist.getId())
            .name(watchlist.getName())
            .description(watchlist.getDescription())
            .tag(watchlist.getTag())
            .notes(watchlist.getNotes())
            .columnPreferences(parseColumnPreferences(watchlist.getColumnPrefs()))
            .isDefault(watchlist.getIsDefault())
            .itemCount(sortedItems.size())
            .items(sortedItems.stream().map(this::mapItemToResponse).collect(Collectors.toList()))
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
            .notes(item.getNotes())
            .displayOrder(item.getDisplayOrder())
            .addedAt(item.getAddedAt())
            .build();
    }
}
