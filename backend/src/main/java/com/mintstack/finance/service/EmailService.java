package com.mintstack.finance.service;

import com.mintstack.finance.config.EmailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Email service for sending notifications.
 * Supports plain text, HTML, and Thymeleaf templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailConfig emailConfig;
    private final TemplateEngine templateEngine;

    /**
     * Send a simple text email
     */
    @Async
    public CompletableFuture<Boolean> sendSimpleEmail(String to, String subject, String text) {
        if (!emailConfig.isEnabled()) {
            log.warn("Email sending is disabled. Skipping email to: {}", to);
            return CompletableFuture.completedFuture(false);
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(formatFromAddress());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Simple email sent to: {}", to);
            return CompletableFuture.completedFuture(true);
        } catch (MailException e) {
            log.error("Failed to send simple email to {}: {}", to, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send an HTML email
     */
    @Async
    public CompletableFuture<Boolean> sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!emailConfig.isEnabled()) {
            log.warn("Email sending is disabled. Skipping email to: {}", to);
            return CompletableFuture.completedFuture(false);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(formatFromAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML email sent to: {}", to);
            return CompletableFuture.completedFuture(true);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send an email using a Thymeleaf template
     */
    @Async
    public CompletableFuture<Boolean> sendTemplateEmail(String to, String subject, 
                                                        String templateName, Map<String, Object> variables) {
        if (!emailConfig.isEnabled()) {
            log.warn("Email sending is disabled. Skipping email to: {}", to);
            return CompletableFuture.completedFuture(false);
        }

        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process("email/" + templateName, context);

            return sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            log.error("Failed to process template {} for email to {}: {}", templateName, to, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send welcome email to new user
     */
    public CompletableFuture<Boolean> sendWelcomeEmail(String to, String userName) {
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "appName", "MintStack Finance"
        );
        return sendTemplateEmail(to, "MintStack Finance'e Hoş Geldiniz!", "welcome", variables);
    }

    /**
     * Send price alert notification
     */
    public CompletableFuture<Boolean> sendPriceAlertEmail(String to, String userName, 
                                                          String symbol, String alertType,
                                                          String targetPrice, String currentPrice) {
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "symbol", symbol,
            "alertType", alertType,
            "targetPrice", targetPrice,
            "currentPrice", currentPrice
        );
        return sendTemplateEmail(to, "Fiyat Alarmı: " + symbol, "price-alert", variables);
    }

    /**
     * Send password reset email
     */
    public CompletableFuture<Boolean> sendPasswordResetEmail(String to, String userName, String resetLink) {
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "resetLink", resetLink,
            "expirationMinutes", 30
        );
        return sendTemplateEmail(to, "Şifre Sıfırlama Talebi", "password-reset", variables);
    }

    /**
     * Format the from address with name
     */
    private String formatFromAddress() {
        return String.format("%s <%s>", emailConfig.getFromName(), emailConfig.getFromAddress());
    }
}
