package com.ft.account.application.port;

public interface EncryptionStrategy {
    String encrypt(String plainText);
    String decrypt(String cipherText);
}
