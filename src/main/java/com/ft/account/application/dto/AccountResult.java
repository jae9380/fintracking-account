package com.ft.account.application.dto;

import com.ft.account.domain.Account;
import com.ft.account.domain.AccountType;

import java.math.BigDecimal;

public record AccountResult(
        Long id,
        String accountName,
        String maskedAccountNumber,
        AccountType accountType,
        BigDecimal balance
) {
    public static AccountResult from(Account account) {
        return new AccountResult(
                account.getId(),
                account.getAccountName(),
                mask(account.getAccountNumber()),
                account.getAccountType(),
                account.getBalance()
        );
    }

    // 계좌번호 마스킹: 앞 4자리만 표시, 나머지 ****
    // ex) 1234-5678-9012 → 1234-****-****
    private static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) return "****";
        return accountNumber.substring(0, 4) + accountNumber.substring(4).replaceAll("[^-]", "*");
    }
}
