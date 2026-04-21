package com.ft.account.application.factory;

import com.ft.account.application.dto.CreateAccountCommand;
import com.ft.account.domain.Account;
import com.ft.account.domain.AccountType;
import com.ft.common.exception.CustomException;

import static com.ft.common.exception.ErrorCode.ACCOUNT_INVALID_TYPE;

public class AccountFactory {

    public static Account create(Long userId, CreateAccountCommand command) {
        validateType(command.accountType());
        return Account.create(
                userId,
                command.accountName(),
                command.accountNumber(),
                command.accountType()
        );
    }

    private static void validateType(AccountType type) {
        if (type == null) {
            throw new CustomException(ACCOUNT_INVALID_TYPE);
        }
    }
}
