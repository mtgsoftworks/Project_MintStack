package com.mintstack.finance.service;

import com.mintstack.finance.config.AlertWebhookSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlertWebhookSecurityServiceTest {

    @Test
    void validateRequest_ShouldPass_WhenSecurityDisabled() {
        AlertWebhookSecurityProperties properties = new AlertWebhookSecurityProperties();
        properties.setEnabled(false);
        AlertWebhookSecurityService service = new AlertWebhookSecurityService(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThatCode(() -> service.validateRequest(request, "{\"status\":\"firing\"}"))
            .doesNotThrowAnyException();
    }

    @Test
    void validateRequest_ShouldReject_WhenIpNotAllowed() {
        AlertWebhookSecurityProperties properties = new AlertWebhookSecurityProperties();
        properties.setEnabled(true);
        properties.setAllowedCidrs(List.of("10.10.0.0/16"));
        AlertWebhookSecurityService service = new AlertWebhookSecurityService(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");

        assertThatThrownBy(() -> service.validateRequest(request, "{\"status\":\"firing\"}"))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("not allowed");
    }

    @Test
    void validateRequest_ShouldReject_WhenSignatureMissing() {
        AlertWebhookSecurityProperties properties = new AlertWebhookSecurityProperties();
        properties.setEnabled(true);
        properties.setRequireSignature(true);
        properties.setSecret("top-secret");
        properties.setSignatureHeader("X-Alert-Signature");
        AlertWebhookSecurityService service = new AlertWebhookSecurityService(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Alert-Signature")).thenReturn(null);

        assertThatThrownBy(() -> service.validateRequest(request, "{\"status\":\"firing\"}"))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("missing");
    }

    @Test
    void validateRequest_ShouldPass_WhenSignatureAndIpAreValid() throws Exception {
        String secret = "top-secret";
        String payload = "{\"status\":\"firing\"}";

        AlertWebhookSecurityProperties properties = new AlertWebhookSecurityProperties();
        properties.setEnabled(true);
        properties.setRequireSignature(true);
        properties.setSecret(secret);
        properties.setSignatureHeader("X-Alert-Signature");
        properties.setSignaturePrefix("sha256=");
        properties.setAllowedCidrs(List.of("127.0.0.1/32"));
        AlertWebhookSecurityService service = new AlertWebhookSecurityService(properties);

        String signature = "sha256=" + hmacSha256Hex(secret, payload);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Alert-Signature")).thenReturn(signature);

        assertThatCode(() -> service.validateRequest(request, payload))
            .doesNotThrowAnyException();
    }

    @Test
    void validateRequest_ShouldIgnoreForwardedFor_WhenProxyIsNotTrusted() {
        AlertWebhookSecurityProperties properties = new AlertWebhookSecurityProperties();
        properties.setEnabled(true);
        properties.setAllowedCidrs(List.of("10.10.0.0/16"));
        properties.setTrustForwardedFor(true);
        properties.setTrustedProxyCidrs(List.of("127.0.0.1/32"));
        AlertWebhookSecurityService service = new AlertWebhookSecurityService(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.11");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.10.1.50");

        assertThatThrownBy(() -> service.validateRequest(request, "{\"status\":\"firing\"}"))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("not allowed");
    }

    @Test
    void validateRequest_ShouldUseForwardedFor_WhenProxyIsTrusted() {
        AlertWebhookSecurityProperties properties = new AlertWebhookSecurityProperties();
        properties.setEnabled(true);
        properties.setAllowedCidrs(List.of("10.10.0.0/16"));
        properties.setTrustForwardedFor(true);
        properties.setTrustedProxyCidrs(List.of("127.0.0.1/32"));
        AlertWebhookSecurityService service = new AlertWebhookSecurityService(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.10.1.50");

        assertThatCode(() -> service.validateRequest(request, "{\"status\":\"firing\"}"))
            .doesNotThrowAnyException();
    }

    private String hmacSha256Hex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
