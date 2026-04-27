package com.ft.account.application;

import com.ft.account.application.dto.AccountResult;
import com.ft.account.application.dto.CreateAccountCommand;
import com.ft.account.application.port.AccountRepository;
import com.ft.account.domain.Account;
import com.ft.account.domain.AccountType;
import com.ft.common.exception.CustomException;
import com.ft.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService 단위 테스트")
class AccountServiceTest {
    @Mock AccountRepository accountRepository;
    @InjectMocks AccountService accountService;

    private Account createAccount(Long userId) {
        return Account.create(userId, "내 통장", "1234-5678-9012", AccountType.CHECKING);
    }

    @Nested
    @DisplayName("계좌 생성")
    class Create {
        @Test
        @DisplayName("성공 - 계좌가 저장되고 결과를 반환한다")
        void create_whenValidCommand_returnsAccountResult() {
            // given
            CreateAccountCommand command = new CreateAccountCommand("내 통장", "1234-5678-9012", AccountType.CHECKING);
            Account saved = createAccount(1L);
            given(accountRepository.save(any(Account.class))).willReturn(saved);

            // when
            AccountResult result = accountService.create(1L, command);

            // then
            assertThat(result.accountName()).isEqualTo("내 통장");
            assertThat(result.accountType()).isEqualTo(AccountType.CHECKING);
        }
    }

    @Nested
    @DisplayName("계좌 목록 조회")
    class FindAll {
        @Test
        @DisplayName("성공 - 유저의 모든 계좌를 반환한다")
        void findAll_whenAccountsExist_returnsAllAccounts() {
            // given
            given(accountRepository.findAllByUserId(1L))
                    .willReturn(List.of(createAccount(1L), createAccount(1L)));

            // when
            List<AccountResult> results = accountService.findAll(1L);

            // then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("성공 - 계좌가 없으면 빈 목록을 반환한다")
        void findAll_whenNoAccounts_returnsEmptyList() {
            // given
            given(accountRepository.findAllByUserId(1L)).willReturn(List.of());

            // when
            List<AccountResult> results = accountService.findAll(1L);

            // then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("계좌 단건 조회")
    class FindById {
        @Test
        @DisplayName("성공 - 본인의 계좌이면 결과를 반환한다")
        void findById_whenValidOwner_returnsAccountResult() {
            // given
            Account account = createAccount(1L);
            given(accountRepository.findById(10L)).willReturn(Optional.of(account));

            // when
            AccountResult result = accountService.findById(1L, 10L);

            // then
            assertThat(result.accountName()).isEqualTo("내 통장");
        }

        @Test
        @DisplayName("실패 - 다른 사용자의 계좌이면 예외 발생")
        void findById_whenWrongOwner_throwsCustomException() {
            // given
            Account account = createAccount(1L);
            given(accountRepository.findById(10L)).willReturn(Optional.of(account));

            // when & then
            assertThatThrownBy(() -> accountService.findById(999L, 10L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ACCOUNT_OWNER_MISMATCH));
        }

        @Test
        @DisplayName("실패 - 계좌가 존재하지 않으면 예외 발생")
        void findById_whenNotFound_throwsCustomException() {
            // given
            given(accountRepository.findById(10L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> accountService.findById(1L, 10L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("계좌 삭제")
    class Delete {

        @Test
        @DisplayName("성공 - 본인의 계좌이면 삭제를 호출한다")
        void delete_whenValidOwner_deletesAccount() {
            // given
            Account account = createAccount(1L);
            given(accountRepository.findById(10L)).willReturn(Optional.of(account));

            // when
            accountService.delete(1L, 10L);

            // then
            then(accountRepository).should().delete(account);
        }

        @Test
        @DisplayName("실패 - 다른 사용자의 계좌이면 예외 발생")
        void delete_whenWrongOwner_throwsCustomException() {
            // given
            Account account = createAccount(1L);
            given(accountRepository.findById(10L)).willReturn(Optional.of(account));

            // when & then
            assertThatThrownBy(() -> accountService.delete(999L, 10L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ACCOUNT_OWNER_MISMATCH));
        }
    }
}
