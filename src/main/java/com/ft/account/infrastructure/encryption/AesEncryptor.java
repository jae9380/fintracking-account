package com.ft.account.infrastructure.encryption;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AesEncryptor {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;

    public static String encrypt(String plainText, byte[] keyBytes) {
        try {
            byte[] iv = generateIv();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new IvParameterSpec(iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 암호문 합쳐서 Base64 인코딩
            byte[] combined = new byte[IV_SIZE + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_SIZE);
            System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("암호화 실패", e);
        }
    }

    public static String decrypt(String cipherText, byte[] keyBytes) {
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            // IV와 암호문 분리
            byte[] iv = new byte[IV_SIZE];
            byte[] encrypted = new byte[combined.length - IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, IV_SIZE);
            System.arraycopy(combined, IV_SIZE, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new IvParameterSpec(iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("복호화 실패", e);
        }
    }

    private static byte[] generateIv() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
