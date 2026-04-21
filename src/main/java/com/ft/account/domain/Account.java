package com.ft.account.domain;

import com.ft.common.entity.BaseEntity;
import com.ft.common.exception.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static com.ft.common.exception.ErrorCode.*;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String accountName;

    @Convert(converter = com.ft.account.infrastructure.encryption.AesEncryptConverter.class)
    @Column(nullable = false)
    private String accountNumber;  // Note: DB 저장 시 AES-256 자동 암복호화

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Column(nullable = false)
    private BigDecimal balance;

    private Account(Long userId, String accountName, String accountNumber, AccountType accountType) {
        this.userId = userId;
        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = BigDecimal.ZERO;
    }

    public static Account create(Long userId, String accountName, String accountNumber, AccountType accountType) {
        validateAccountName(accountName);
        validateAccountNumber(accountNumber);
        return new Account(userId, accountName, accountNumber, accountType);
    }

    // 소유자 검증
    public void validateOwner(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new CustomException(ACCOUNT_OWNER_MISMATCH);
        }
    }

    // 계좌명 변경
    public void updateName(String newName) {
        validateAccountName(newName);
        this.accountName = newName;
    }

    // 입금
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ACCOUNT_INVALID_AMOUNT);
        }
        this.balance = this.balance.add(amount);
    }

    // 출금
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ACCOUNT_INVALID_AMOUNT);
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new CustomException(ACCOUNT_INSUFFICIENT_BALANCE);
        }
        this.balance = this.balance.subtract(amount);
    }

    private static void validateAccountName(String name) {
        if (name == null || name.isBlank()) {
            throw new CustomException(ACCOUNT_INVALID_NAME);
        }
    }

    private static void validateAccountNumber(String number) {
        if (number == null || number.isBlank()) {
            throw new CustomException(ACCOUNT_INVALID_NUMBER);
        }
    }
}
