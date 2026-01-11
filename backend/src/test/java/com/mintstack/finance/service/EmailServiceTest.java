package com.mintstack.finance.service;

import com.mintstack.finance.config.EmailConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailConfig emailConfig;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        lenient().when(emailConfig.getFromAddress()).thenReturn("noreply@mintstack.com");
        lenient().when(emailConfig.getFromName()).thenReturn("MintStack Finance");
    }

    @Test
    void sendSimpleEmail_ShouldReturnFalse_WhenDisabled() throws ExecutionException, InterruptedException {
        // Given
        when(emailConfig.isEnabled()).thenReturn(false);

        // When
        CompletableFuture<Boolean> result = emailService.sendSimpleEmail("test@example.com", "Subject", "Body");

        // Then
        assertThat(result.get()).isFalse();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendSimpleEmail_ShouldSendEmail_WhenEnabled() throws ExecutionException, InterruptedException {
        // Given
        when(emailConfig.isEnabled()).thenReturn(true);

        // When
        CompletableFuture<Boolean> result = emailService.sendSimpleEmail("test@example.com", "Subject", "Body");

        // Then
        assertThat(result.get()).isTrue();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendHtmlEmail_ShouldReturnFalse_WhenDisabled() throws ExecutionException, InterruptedException {
        // Given
        when(emailConfig.isEnabled()).thenReturn(false);

        // When
        CompletableFuture<Boolean> result = emailService.sendHtmlEmail("test@example.com", "Subject", "<h1>HTML</h1>");

        // Then
        assertThat(result.get()).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendHtmlEmail_ShouldSendEmail_WhenEnabled() throws ExecutionException, InterruptedException {
        // Given
        when(emailConfig.isEnabled()).thenReturn(true);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        CompletableFuture<Boolean> result = emailService.sendHtmlEmail("test@example.com", "Subject", "<h1>HTML</h1>");

        // Then
        assertThat(result.get()).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendTemplateEmail_ShouldReturnFalse_WhenDisabled() throws ExecutionException, InterruptedException {
        // Given
        when(emailConfig.isEnabled()).thenReturn(false);

        // When
        CompletableFuture<Boolean> result = emailService.sendTemplateEmail(
            "test@example.com", "Subject", "welcome", Map.of("userName", "Test")
        );

        // Then
        assertThat(result.get()).isFalse();
    }

    @Test
    void sendWelcomeEmail_ShouldProcessTemplate() throws ExecutionException, InterruptedException {
        // Given
        when(emailConfig.isEnabled()).thenReturn(true);
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<h1>Welcome</h1>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        CompletableFuture<Boolean> result = emailService.sendWelcomeEmail("test@example.com", "Test User");

        // Then
        assertThat(result.get()).isTrue();
    }

    @Test
    void sendPriceAlertEmail_ShouldProcessTemplate() throws ExecutionException, InterruptedException {
        // Given
        when(emailConfig.isEnabled()).thenReturn(true);
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<h1>Alert</h1>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        CompletableFuture<Boolean> result = emailService.sendPriceAlertEmail(
            "test@example.com", "Test User", "USD/TRY", "PRICE_ABOVE", "35.00", "34.50"
        );

        // Then
        assertThat(result.get()).isTrue();
    }

    @Test
    void sendPasswordResetEmail_ShouldProcessTemplate() throws ExecutionException, InterruptedException {
        // Given
        when(emailConfig.isEnabled()).thenReturn(true);
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<h1>Reset</h1>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        CompletableFuture<Boolean> result = emailService.sendPasswordResetEmail(
            "test@example.com", "Test User", "https://example.com/reset?token=abc"
        );

        // Then
        assertThat(result.get()).isTrue();
    }
}
