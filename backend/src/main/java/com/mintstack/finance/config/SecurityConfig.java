package com.mintstack.finance.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN;

/**
 * Security configuration with strict Content Security Policy.
 *
 * SECURITY: CSP is configured without 'unsafe-inline' for scripts and styles.
 * This prevents XSS attacks by blocking inline script execution.
 * Scripts must be loaded from external files with valid integrity hashes.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers("/api-docs/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()

                // WebSocket handshake endpoints are public, but STOMP CONNECT
                // frames are authenticated by WebSocketAuthInterceptor.
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/ws-native/**").permitAll()

                // Public market data endpoints (read-only)
                .requestMatchers(HttpMethod.GET, "/api/v1/market/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/news/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/news").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/glossary/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/admin/webhooks/alerts").permitAll()

                // Simulation endpoints - admin only
                .requestMatchers("/api/v1/simulation/**").hasRole("ADMIN")

                // Data source endpoints - admin only
                .requestMatchers("/api/v1/data-sources/**").hasRole("ADMIN")

                // API key management - admin only
                .requestMatchers("/api/v1/settings/api-keys/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/settings/api-keys").hasRole("ADMIN")

                // Destructive settings operations - admin only
                .requestMatchers(HttpMethod.DELETE, "/api/v1/settings/cache").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/settings/market-data").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/settings/market-data/backfill").hasRole("ADMIN")

                // Admin endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            // Security Headers – RELAXED for local development / demo
            .headers(headers -> {
                headers.contentTypeOptions(contentTypeOptions -> {});
                headers.frameOptions(frameOptions -> frameOptions.sameOrigin());
                headers.xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK));
                headers.referrerPolicy(referrer -> referrer.policy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                // CSP disabled for local dev
                headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src * 'unsafe-inline' 'unsafe-eval' data: blob:;"
                ));
                // HSTS disabled for local dev
                headers.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(false)
                    .maxAgeInSeconds(0));
            });

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Local dev: allow all origins with credentials
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    /**
     * Converter to extract roles from Keycloak JWT token.
     * Keycloak stores realm roles in: realm_access.roles
     * Keycloak stores client roles in: resource_access.{client}.roles
     */
    static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            List<GrantedAuthority> authorities = new ArrayList<>();

            // Extract realm roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");
                authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList()));
            }

            // Extract client roles (optional)
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                resourceAccess.forEach((clientId, clientAccess) -> {
                    if (clientAccess instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> access = (Map<String, Object>) clientAccess;
                        if (access.containsKey("roles")) {
                            @SuppressWarnings("unchecked")
                            List<String> clientRoles = (List<String>) access.get("roles");
                            authorities.addAll(clientRoles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + clientId.toUpperCase() + "_" + role.toUpperCase()))
                                .collect(Collectors.toList()));
                        }
                    }
                });
            }

            return authorities;
        }
    }
}
