package com.ft.account.infrastructure.persistence;

import com.ft.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaAccountRepository extends JpaRepository<Account, Long> {
    List<Account> findAllByUserId(Long userId);
}
