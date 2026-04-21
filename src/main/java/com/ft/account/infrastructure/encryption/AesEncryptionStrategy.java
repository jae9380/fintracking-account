package com.ft.account.infrastructure.encryption;

import com.ft.account.application.port.EncryptionStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class AesEncryptionStrategy implements EncryptionStrategy {

    private final byte[] keyBytes;

    public AesEncryptionStrategy(@Value("${encryption.aes-key}") String aesKey) {
        this.keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String encrypt(String plainText) {
        return AesEncryptor.encrypt(plainText, keyBytes);
    }

    @Override
    public String decrypt(String cipherText) {
        return AesEncryptor.decrypt(cipherText, keyBytes);
    }
}
