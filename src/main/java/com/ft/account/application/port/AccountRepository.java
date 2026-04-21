package com.ft.account.application.port;

import com.ft.account.domain.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(Long id);
    List<Account> findAllByUserId(Long userId);
    void delete(Account account);
}
