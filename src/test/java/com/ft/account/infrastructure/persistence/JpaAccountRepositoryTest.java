package com.ft.account.infrastructure.persistence;

import com.ft.account.domain.Account;
import com.ft.account.domain.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JpaAccountRepository 테스트")
class JpaAccountRepositoryTest {
    @Autowired
    JpaAccountRepository jpaAccountRepository;

    @Nested
    @DisplayName("유저 ID로 계좌 목록 조회")
    class FindAllByUserId {
        @Test
        @DisplayName("성공 - 해당 유저의 계좌만 반환한다")
        void findAllByUserId_whenAccountsExist_returnsOnlyUserAccounts() {
            // given
            Account account1 = Account.create(1L, "통장A", "1111-2222-3333", AccountType.CHECKING);
            Account account2 = Account.create(1L, "통장B", "4444-5555-6666", AccountType.SAVINGS);
            Account otherUserAccount = Account.create(2L, "타인 통장", "7777-8888-9999", AccountType.CHECKING);
            jpaAccountRepository.saveAll(List.of(account1, account2, otherUserAccount));

            // when
            List<Account> results = jpaAccountRepository.findAllByUserId(1L);

            // then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(a -> a.getUserId().equals(1L));
        }

        @Test
        @DisplayName("성공 - 계좌가 없으면 빈 목록을 반환한다")
        void findAllByUserId_whenNoAccounts_returnsEmptyList() {
            // when
            List<Account> results = jpaAccountRepository.findAllByUserId(999L);

            // then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("AES 암복호화")
    class Encryption {
        @Test
        @DisplayName("성공 - 저장 시 암호화되고 조회 시 복호화된다")
        void save_whenAccountSaved_encryptsAndDecryptsAccountNumber() {
            // given
            Account account = Account.create(1L, "내 통장", "1234-5678-9012", AccountType.CHECKING);
            jpaAccountRepository.save(account);
            jpaAccountRepository.flush();

            // when
            Account found = jpaAccountRepository.findById(account.getId()).orElseThrow();

            // then
            assertThat(found.getAccountNumber()).isEqualTo("1234-5678-9012");
        }
    }
}
