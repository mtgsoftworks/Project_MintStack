package com.mintstack.finance.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Configuration for API versioning headers and documentation.
 * 
 * API Versioning Policy:
 * - URL-based versioning: /api/v1/*, /api/v2/*, etc.
 * - Current version: v1
 * - Deprecated versions are supported for 6 months after deprecation
 * - Version information is included in response headers
 * 
 * Response Headers:
 * - X-API-Version: Current API version being used
 * - X-API-Deprecated: true if the version is deprecated
 * - X-API-Sunset: Date when deprecated version will be removed
 */
@Configuration
public class ApiVersioningConfig {

    public static final String CURRENT_API_VERSION = "1.0.0";
    public static final String API_VERSION_HEADER = "X-API-Version";
    public static final String API_DEPRECATED_HEADER = "X-API-Deprecated";
    public static final String API_SUNSET_HEADER = "X-API-Sunset";
    public static final String API_MIN_VERSION_HEADER = "X-API-Min-Version";

    @Bean
    public FilterRegistrationBean<ApiVersionFilter> apiVersionFilter() {
        FilterRegistrationBean<ApiVersionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiVersionFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }

    /**
     * Filter that adds API version headers to all responses.
     */
    public static class ApiVersionFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            
            // Add version headers to response
            response.setHeader(API_VERSION_HEADER, CURRENT_API_VERSION);
            response.setHeader(API_MIN_VERSION_HEADER, "1.0.0");
            
            // Check if the request is for a deprecated version
            String requestPath = request.getRequestURI();
            if (isDeprecatedVersion(requestPath)) {
                response.setHeader(API_DEPRECATED_HEADER, "true");
                response.setHeader(API_SUNSET_HEADER, getDeprecationSunsetDate(requestPath));
            }
            
            filterChain.doFilter(request, response);
        }

        private boolean isDeprecatedVersion(String path) {
            // No deprecated versions yet - v1 is current
            // When v2 is released, v1 will be marked as deprecated
            return false;
        }

        private String getDeprecationSunsetDate(String path) {
            // Return the sunset date for deprecated versions
            // Format: RFC 7231 HTTP-date
            return null;
        }
    }
}
