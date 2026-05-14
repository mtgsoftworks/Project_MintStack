package com.mintstack.finance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.alert-webhook")
public class AlertWebhookSecurityProperties {

    private boolean enabled = true;
    private boolean requireSignature = false;
    private String secret = "";
    private String signatureHeader = "X-Alert-Signature";
    private String signaturePrefix = "sha256=";
    private boolean trustForwardedFor = false;
    private List<String> trustedProxyCidrs = new ArrayList<>();
    private List<String> allowedCidrs = new ArrayList<>();
}
