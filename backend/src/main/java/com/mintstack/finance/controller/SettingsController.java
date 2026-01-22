package com.mintstack.finance.controller;

import com.mintstack.finance.dto.request.ApiConfigRequest;
import com.mintstack.finance.dto.response.ApiConfigResponse;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.service.ApiKeyValidationService;
import com.mintstack.finance.service.MarketDataService;
import com.mintstack.finance.service.SettingsService;
import com.mintstack.finance.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Settings", description = "Uygulama ayarları ve API yapılandırması")
@SecurityRequirement(name = "bearer")
public class SettingsController {

    private final SettingsService settingsService;
    private final UserService userService;
    private final CacheManager cacheManager;
    private final MarketDataService marketDataService;

    @GetMapping("/api-keys")
    @Operation(summary = "Kullanıcının API anahtarlarını listele")
    public ResponseEntity<ApiResponse<List<ApiConfigResponse>>> getApiConfigs(
            @AuthenticationPrincipal Jwt jwt) {
        
        User user = userService.getUserByKeycloakId(jwt.getSubject());
        List<ApiConfigResponse> configs = settingsService.getApiConfigs(user.getId());
        
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    @GetMapping("/api-keys/providers")
    @Operation(summary = "API sağlayıcıları ve varsayılan URL'lerini listele")
    public ResponseEntity<ApiResponse<Map<UserApiConfig.ApiProvider, String>>> getProviders() {
        Map<UserApiConfig.ApiProvider, String> providers = settingsService.getDefaultUrls();
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @PostMapping("/api-keys/test")
    @Operation(summary = "API anahtarını kaydetmeden önce test et")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testApiKey(
            @Valid @RequestBody ApiConfigRequest request) {
        
        ApiKeyValidationService.ValidationResult result = settingsService.validateApiKey(request);
        
        Map<String, Object> response = Map.of(
                "valid", result.isValid(),
                "message", result.getMessage()
        );
        
        if (result.isValid()) {
            return ResponseEntity.ok(ApiResponse.success(response, result.getMessage()));
        } else {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message(result.getMessage())
                            .data(response)
                            .build()
            );
        }
    }

    @PostMapping("/api-keys")
    @Operation(summary = "Yeni API anahtarı ekle (önce test edilir)")
    public ResponseEntity<ApiResponse<ApiConfigResponse>> addApiConfig(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ApiConfigRequest request) {
        
        User user = userService.getOrCreateUser(jwt);
        
        try {
            ApiConfigResponse config = settingsService.addApiConfig(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success(config, "API anahtarı doğrulandı ve kaydedildi ✓"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<ApiConfigResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @DeleteMapping("/api-keys/{id}")
    @Operation(summary = "API anahtarını sil")
    public ResponseEntity<ApiResponse<Void>> deleteApiConfig(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        
        User user = userService.getUserByKeycloakId(jwt.getSubject());
        settingsService.deleteApiConfig(user.getId(), id);
        
        return ResponseEntity.ok(ApiResponse.success(null, "API ayarı silindi"));
    }

    @DeleteMapping("/cache")
    @Operation(summary = "Tüm uygulama önbelleğini temizle (Redis cache)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clearCache(
            @AuthenticationPrincipal Jwt jwt) {
        
        int clearedCaches = 0;
        var cacheNames = cacheManager.getCacheNames();
        
        for (String cacheName : cacheNames) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                clearedCaches++;
                log.info("Cache cleared: {}", cacheName);
            }
        }
        
        log.info("Total caches cleared: {} by user: {}", clearedCaches, jwt.getSubject());
        
        Map<String, Object> result = Map.of(
                "clearedCaches", clearedCaches,
                "cacheNames", cacheNames
        );
        
        return ResponseEntity.ok(ApiResponse.success(result, "Önbellek temizlendi"));
    }

    @DeleteMapping("/market-data")
    @Operation(summary = "Tüm piyasa verilerini sil (döviz kurları, fiyat geçmişi)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteMarketData(
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("User {} requested to delete all market data", jwt.getSubject());
        
        Map<String, Object> result = marketDataService.deleteAllMarketData();
        
        // Also clear cache
        var cacheNames = cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success(result, "Tüm piyasa verileri silindi"));
    }
}

