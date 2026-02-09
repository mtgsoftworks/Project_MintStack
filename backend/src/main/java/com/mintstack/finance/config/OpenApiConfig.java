package com.mintstack.finance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MintStack Finance Portal API")
                .version(ApiVersioningConfig.CURRENT_API_VERSION)
                .description("""
                    RESTful API for MintStack Finance Portal.
                    
                    ## Features
                    - Real-time market data (currencies, stocks, bonds, funds, VIOP)
                    - Financial news aggregation
                    - Portfolio management with profit/loss tracking
                    - Technical analysis tools (MA, trends)
                    
                    ## Authentication
                    This API uses OAuth2/OpenID Connect via Keycloak.
                    Use the 'Authorize' button to authenticate.
                    
                    ## API Versioning
                    
                    ### URL-Based Versioning
                    All API endpoints are versioned using URL path prefixes:
                    - **v1**: `/api/v1/*` (Current stable version)
                    
                    ### Response Headers
                    All API responses include the following headers:
                    - `X-API-Version`: Current API version (e.g., "1.0.0")
                    - `X-API-Min-Version`: Minimum supported API version
                    - `X-API-Deprecated`: Present if version is deprecated
                    - `X-API-Sunset`: Date when deprecated version will be removed
                    
                    ### Deprecation Policy
                    - Deprecated versions are supported for 6 months after deprecation notice
                    - Breaking changes only occur in major version increments
                    - Minor versions add backwards-compatible features
                    - Patch versions include bug fixes only
                    """)
                .contact(new Contact()
                    .name("MintStack Team")
                    .email("support@mintstack.local")
                    .url("https://mintstack.local"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development Server"),
                new Server()
                    .url("http://backend:8080")
                    .description("Docker Container")))
            .components(new Components()
                .addSecuritySchemes("oauth2", new SecurityScheme()
                    .type(SecurityScheme.Type.OAUTH2)
                    .description("OAuth2 authentication via Keycloak")
                    .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                            .authorizationUrl(issuerUri + "/protocol/openid-connect/auth")
                            .tokenUrl(issuerUri + "/protocol/openid-connect/token")
                            .refreshUrl(issuerUri + "/protocol/openid-connect/token"))))
                .addSecuritySchemes("bearer", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT Bearer token")))
            .addSecurityItem(new SecurityRequirement()
                .addList("bearer"));
    }
}
