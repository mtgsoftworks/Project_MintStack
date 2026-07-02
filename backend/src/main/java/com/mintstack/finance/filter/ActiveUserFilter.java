package com.mintstack.finance.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.ErrorResponse;
import com.mintstack.finance.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@Order(Ordered.LOWEST_PRECEDENCE + 50)
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = "app.security.active-user-check-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ActiveUserFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Jwt jwt) {
            boolean inactive = userRepository.findActiveStatusByKeycloakId(jwt.getSubject())
                    .map(Boolean.FALSE::equals)
                    .orElse(false);
            if (inactive) {
                denyAccess(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void denyAccess(HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("USER_INACTIVE")
                .message("User account is inactive")
                .timestamp(LocalDateTime.now())
                .build();

        try {
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(error)));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write inactive-user response", exception);
        }
    }
}
