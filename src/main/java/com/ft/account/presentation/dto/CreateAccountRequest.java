package com.ft.account.presentation.dto;

import com.ft.account.application.dto.CreateAccountCommand;
import com.ft.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotBlank
        String accountName,

        @NotBlank
        String accountNumber,

        @NotNull
        AccountType accountType
) {
    public CreateAccountCommand toCommand() {
        return new CreateAccountCommand(accountName, accountNumber, accountType);
    }
}
