package com.mintstack.finance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${app.security.jwt.audience:finance-backend}") String audience,
            @Value("${app.security.jwt.allowed-authorized-parties:finance-frontend}") String authorizedParties) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> clientValidator =
                new AudienceOrAuthorizedPartyValidator(audience, authorizedParties);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, clientValidator));
        return decoder;
    }

    static final class AudienceOrAuthorizedPartyValidator implements OAuth2TokenValidator<Jwt> {

        private static final OAuth2Error INVALID_CLIENT = new OAuth2Error(
                "invalid_token",
                "JWT is not intended for the MintStack backend",
                null
        );

        private final String requiredAudience;
        private final Set<String> allowedAuthorizedParties;

        AudienceOrAuthorizedPartyValidator(String requiredAudience, String authorizedParties) {
            this.requiredAudience = requiredAudience;
            this.allowedAuthorizedParties = Arrays.stream(authorizedParties.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            boolean audienceMatches = jwt.getAudience() != null
                    && jwt.getAudience().contains(requiredAudience);
            String authorizedParty = jwt.getClaimAsString("azp");
            boolean authorizedPartyMatches = authorizedParty != null
                    && allowedAuthorizedParties.contains(authorizedParty);

            return audienceMatches || authorizedPartyMatches
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(INVALID_CLIENT);
        }
    }
}
