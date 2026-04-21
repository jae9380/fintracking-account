package com.ft.account.application.dto;

import com.ft.account.domain.AccountType;

public record CreateAccountCommand(
        String accountName,
        String accountNumber,
        AccountType accountType
) {}
