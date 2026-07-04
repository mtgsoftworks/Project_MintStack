package com.mintstack.finance.service;

import com.mintstack.finance.config.AlertWebhookSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertWebhookSecurityService {

    private final AlertWebhookSecurityProperties properties;

    public void validateRequest(HttpServletRequest request, String payloadBody) {
        if (!properties.isEnabled()) {
            return;
        }

        String clientIp = resolveClientIp(request);
        validateSourceIp(clientIp);
        validateSignature(payloadBody, request.getHeader(properties.getSignatureHeader()));
    }

    private void validateSourceIp(String clientIp) {
        List<String> allowlist = properties.getAllowedCidrs().stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();

        if (allowlist.isEmpty()) {
            return;
        }

        boolean matched = allowlist.stream().anyMatch(range -> matchesIpRange(range, clientIp));
        if (!matched) {
            throw new AccessDeniedException("Webhook source IP is not allowed");
        }
    }

    private boolean matchesIpRange(String range, String clientIp) {
        try {
            return new IpAddressMatcher(range).matches(clientIp);
        } catch (Exception e) {
            return range.equals(clientIp);
        }
    }

    private void validateSignature(String payloadBody, String providedHeaderValue) {
        if (!properties.isRequireSignature()) {
            return;
        }
        if (!StringUtils.hasText(properties.getSecret())) {
            throw new AccessDeniedException("Webhook signature secret is not configured");
        }
        if (!StringUtils.hasText(providedHeaderValue)) {
            throw new AccessDeniedException("Webhook signature header is missing");
        }

        String expectedHex = hmacSha256Hex(properties.getSecret(), payloadBody == null ? "" : payloadBody);
        String withPrefix = defaultPrefix() + expectedHex;

        String normalizedHeader = providedHeaderValue.trim();
        boolean exactMatch = equalsConstantTime(normalizedHeader, expectedHex);
        boolean prefixedMatch = equalsConstantTime(normalizedHeader, withPrefix);

        if (!exactMatch && !prefixedMatch) {
            throw new AccessDeniedException("Webhook signature validation failed");
        }
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to compute webhook HMAC signature: {}", e.getMessage());
            throw new IllegalStateException("Webhook signature computation failed");
        }
    }

    private boolean equalsConstantTime(String left, String right) {
        return MessageDigest.isEqual(
            left.getBytes(StandardCharsets.UTF_8),
            right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!properties.isTrustForwardedFor()) {
            return remoteAddr;
        }

        List<String> trustedProxies = properties.getTrustedProxyCidrs().stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();

        if (trustedProxies.isEmpty()) {
            log.debug("trustForwardedFor is enabled but trustedProxyCidrs is empty. Falling back to remote address.");
            return remoteAddr;
        }

        boolean viaTrustedProxy = trustedProxies.stream().anyMatch(range -> matchesIpRange(range, remoteAddr));
        if (!viaTrustedProxy) {
            return remoteAddr;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String firstHop = forwardedFor.split(",")[0].trim();
            if (StringUtils.hasText(firstHop)) {
                return firstHop;
            }
        }
        return remoteAddr;
    }

    private String defaultPrefix() {
        return StringUtils.hasText(properties.getSignaturePrefix())
            ? properties.getSignaturePrefix().trim()
            : "sha256=";
    }
}
