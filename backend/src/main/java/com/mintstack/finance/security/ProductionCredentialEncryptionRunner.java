package com.mintstack.finance.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class ProductionCredentialEncryptionRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        EncryptedStringConverter.requireValidProductionKey();

        int[] migrated = jdbcTemplate.query(
                "SELECT id, api_key, secret_key FROM user_api_configs",
                (resultSet, rowNumber) -> new CredentialRow(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("api_key"),
                        resultSet.getString("secret_key")
                )
        ).stream()
                .filter(row -> !EncryptedStringConverter.isEncrypted(row.apiKey())
                        || !EncryptedStringConverter.isEncrypted(row.secretKey()))
                .mapToInt(this::encryptRow)
                .toArray();

        if (migrated.length > 0) {
            log.info("Encrypted {} legacy API credential records", migrated.length);
        }
    }

    private int encryptRow(CredentialRow row) {
        return jdbcTemplate.update(
                "UPDATE user_api_configs SET api_key = ?, secret_key = ? WHERE id = ?",
                EncryptedStringConverter.encryptForMigration(row.apiKey()),
                EncryptedStringConverter.encryptForMigration(row.secretKey()),
                row.id()
        );
    }

    private record CredentialRow(UUID id, String apiKey, String secretKey) {
    }
}
