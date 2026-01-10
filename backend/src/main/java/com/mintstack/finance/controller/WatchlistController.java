package com.mintstack.finance.controller;

import com.mintstack.finance.dto.request.CreateWatchlistRequest;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.WatchlistResponse;
import com.mintstack.finance.service.WatchlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WatchlistResponse>>> getUserWatchlists(
            @AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        List<WatchlistResponse> watchlists = watchlistService.getUserWatchlists(keycloakId);
        return ResponseEntity.ok(ApiResponse.success(watchlists));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WatchlistResponse>> getWatchlist(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        String keycloakId = jwt.getSubject();
        WatchlistResponse watchlist = watchlistService.getWatchlist(keycloakId, id);
        return ResponseEntity.ok(ApiResponse.success(watchlist));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WatchlistResponse>> createWatchlist(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateWatchlistRequest request) {
        String keycloakId = jwt.getSubject();
        WatchlistResponse watchlist = watchlistService.createWatchlist(keycloakId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(watchlist));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WatchlistResponse>> updateWatchlist(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody CreateWatchlistRequest request) {
        String keycloakId = jwt.getSubject();
        WatchlistResponse watchlist = watchlistService.updateWatchlist(keycloakId, id, request);
        return ResponseEntity.ok(ApiResponse.success(watchlist));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWatchlist(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        String keycloakId = jwt.getSubject();
        watchlistService.deleteWatchlist(keycloakId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Watchlist başarıyla silindi"));
    }

    @PostMapping("/{id}/items/{symbol}")
    public ResponseEntity<ApiResponse<WatchlistResponse>> addInstrument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable String symbol) {
        String keycloakId = jwt.getSubject();
        WatchlistResponse watchlist = watchlistService.addInstrument(keycloakId, id, symbol);
        return ResponseEntity.ok(ApiResponse.success(watchlist, "Enstrüman watchlist'e eklendi"));
    }

    @DeleteMapping("/{id}/items/{symbol}")
    public ResponseEntity<ApiResponse<WatchlistResponse>> removeInstrument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable String symbol) {
        String keycloakId = jwt.getSubject();
        WatchlistResponse watchlist = watchlistService.removeInstrument(keycloakId, id, symbol);
        return ResponseEntity.ok(ApiResponse.success(watchlist, "Enstrüman watchlist'ten kaldırıldı"));
    }
}
