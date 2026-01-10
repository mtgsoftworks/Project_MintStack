package com.mintstack.finance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Properties;

/**
 * Email configuration using Spring Mail.
 * Supports SMTP, Gmail, SendGrid, AWS SES.
 */
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "app.email")
@Getter
@Setter
public class EmailConfig {

    /**
     * Enable/disable email sending
     */
    private boolean enabled = true;

    /**
     * SMTP host
     */
    private String host = "smtp.gmail.com";

    /**
     * SMTP port
     */
    private int port = 587;

    /**
     * SMTP username
     */
    private String username;

    /**
     * SMTP password
     */
    private String password;

    /**
     * From email address
     */
    private String fromAddress = "noreply@mintstack.com";

    /**
     * From name
     */
    private String fromName = "MintStack Finance";

    /**
     * Enable TLS
     */
    private boolean tlsEnabled = true;

    /**
     * Enable SSL
     */
    private boolean sslEnabled = false;

    /**
     * Debug mode
     */
    private boolean debug = false;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        
        if (tlsEnabled) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        
        if (sslEnabled) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        
        props.put("mail.debug", String.valueOf(debug));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return mailSender;
    }
}
