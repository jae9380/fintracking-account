package com.ft.account.infrastructure.persistence;

import com.ft.account.application.port.AccountRepository;
import com.ft.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final JpaAccountRepository jpaAccountRepository;

    @Override
    public Account save(Account account) {
        return jpaAccountRepository.save(account);
    }

    @Override
    public Optional<Account> findById(Long id) {
        return jpaAccountRepository.findById(id);
    }

    @Override
    public List<Account> findAllByUserId(Long userId) {
        return jpaAccountRepository.findAllByUserId(userId);
    }

    @Override
    public void delete(Account account) {
        jpaAccountRepository.delete(account);
    }
}
