package com.ft.account.infrastructure.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Converter
@Component
public class AesEncryptConverter implements AttributeConverter<String, String> {

    private static byte[] keyBytes;

    @Value("${encryption.aes-key}")
    public void init(String aesKey) {
        AesEncryptConverter.keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null) return null;
        return AesEncryptor.encrypt(plainText, keyBytes);
    }

    @Override
    public String convertToEntityAttribute(String cipherText) {
        if (cipherText == null) return null;
        return AesEncryptor.decrypt(cipherText, keyBytes);
    }
}
