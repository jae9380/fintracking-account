package com.ft.account.presentation.dto;

import com.ft.account.application.dto.AccountResult;
import com.ft.account.domain.AccountType;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String accountName,
        String accountNumber,
        AccountType accountType,
        BigDecimal balance
) {
    public static AccountResponse from(AccountResult result) {
        return new AccountResponse(
                result.id(),
                result.accountName(),
                result.maskedAccountNumber(),
                result.accountType(),
                result.balance()
        );
    }
}
