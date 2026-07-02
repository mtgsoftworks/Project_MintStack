package com.mintstack.finance.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    public static final String KEY_ENVIRONMENT_VARIABLE = "APP_FIELD_ENCRYPTION_KEY";
    private static final String PREFIX = "enc:v1:";
    private static final byte[] AAD = "mintstack:user-api-config:v1".getBytes(StandardCharsets.UTF_8);
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.startsWith(PREFIX)) {
            return attribute;
        }
        SecretKey key = configuredKey(false);
        return key == null ? attribute : encrypt(attribute, key);
    }

    @Override
    public String convertToEntityAttribute(String databaseValue) {
        if (databaseValue == null || !databaseValue.startsWith(PREFIX)) {
            return databaseValue;
        }
        return decrypt(databaseValue, configuredKey(true));
    }

    public static boolean isEncrypted(String value) {
        return value == null || value.startsWith(PREFIX);
    }

    public static String encryptForMigration(String value) {
        if (value == null || value.startsWith(PREFIX)) {
            return value;
        }
        return encrypt(value, configuredKey(true));
    }

    public static void requireValidProductionKey() {
        configuredKey(true);
    }

    private static String encrypt(String plaintext, SecretKey key) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(AAD);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload = ByteBuffer.allocate(nonce.length + ciphertext.length)
                    .put(nonce)
                    .put(ciphertext)
                    .array();
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt sensitive configuration", exception);
        }
    }

    private static String decrypt(String encryptedValue, SecretKey key) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue.substring(PREFIX.length()));
            if (payload.length <= NONCE_LENGTH_BYTES) {
                throw new IllegalArgumentException("Encrypted configuration payload is invalid");
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            buffer.get(nonce);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(AAD);
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt sensitive configuration", exception);
        }
    }

    private static SecretKey configuredKey(boolean required) {
        String encodedKey = System.getProperty(KEY_ENVIRONMENT_VARIABLE);
        if (encodedKey == null || encodedKey.isBlank()) {
            encodedKey = System.getenv(KEY_ENVIRONMENT_VARIABLE);
        }
        if (encodedKey == null || encodedKey.isBlank()) {
            if (required) {
                throw new IllegalStateException(KEY_ENVIRONMENT_VARIABLE + " must be configured");
            }
            return null;
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey.trim());
            if (keyBytes.length != 32) {
                throw new IllegalStateException(
                        KEY_ENVIRONMENT_VARIABLE + " must be a Base64-encoded 256-bit key");
            }
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    KEY_ENVIRONMENT_VARIABLE + " must be valid Base64", exception);
        }
    }
}
