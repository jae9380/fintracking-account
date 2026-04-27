package com.ft.account.application;

import com.ft.account.application.port.AccountRepository;
import com.ft.account.domain.Account;
import com.ft.account.domain.AccountType;
import com.ft.common.event.TransactionCreatedEvent;
import com.ft.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.ft.common.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceUpdateEventHandler 단위 테스트")
class BalanceUpdateEventHandlerTest {
    @Mock AccountRepository accountRepository;
    @InjectMocks BalanceUpdateEventHandler handler;

    private Account accountWithBalance(Long userId, BigDecimal initialBalance) {
        Account account = Account.create(userId, "테스트 계좌", "1234-0000-0001", AccountType.CHECKING);
        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            account.deposit(initialBalance);
        }
        return account;
    }

    private TransactionCreatedEvent event(String type, Long accountId, Long toAccountId, BigDecimal amount) {
        return new TransactionCreatedEvent(
                "event-id", 1L, accountId, toAccountId, 100L, amount, type, null, null, LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("수입 이벤트 처리")
    class HandleIncome {
        @Test
        @DisplayName("성공 - 수입 이벤트 수신 시 계좌 잔액이 증가한다")
        void handle_whenIncome_depositsAmountToAccount() {
            // given
            Account account = accountWithBalance(1L, BigDecimal.ZERO);
            given(accountRepository.findById(1L)).willReturn(Optional.of(account));

            // when
            handler.handle(event("INCOME", 1L, null, new BigDecimal("5000")));

            // then
            assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("5000"));
        }
    }

    @Nested
    @DisplayName("지출 이벤트 처리")
    class HandleExpense {
        @Test
        @DisplayName("성공 - 지출 이벤트 수신 시 계좌 잔액이 감소한다")
        void handle_whenExpense_withdrawsAmountFromAccount() {
            // given
            Account account = accountWithBalance(1L, new BigDecimal("10000"));
            given(accountRepository.findById(1L)).willReturn(Optional.of(account));

            // when
            handler.handle(event("EXPENSE", 1L, null, new BigDecimal("3000")));

            // then
            assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("7000"));
        }

        @Test
        @DisplayName("실패 - 잔액 부족이면 예외 발생")
        void handle_whenInsufficientBalance_throwsCustomException() {
            // given
            Account account = accountWithBalance(1L, new BigDecimal("500"));
            given(accountRepository.findById(1L)).willReturn(Optional.of(account));

            // when & then
            assertThatThrownBy(() -> handler.handle(event("EXPENSE", 1L, null, new BigDecimal("1000"))))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ACCOUNT_INSUFFICIENT_BALANCE));
        }
    }

    @Nested
    @DisplayName("이체 이벤트 처리")
    class HandleTransfer {
        @Test
        @DisplayName("성공 - 이체 이벤트 수신 시 출금 계좌에서 차감되고 입금 계좌에 더해진다")
        void handle_whenTransfer_withdrawsFromSourceAndDepositsToTarget() {
            // given
            Account source = accountWithBalance(1L, new BigDecimal("10000"));
            Account target = accountWithBalance(1L, BigDecimal.ZERO);
            given(accountRepository.findById(1L)).willReturn(Optional.of(source));
            given(accountRepository.findById(2L)).willReturn(Optional.of(target));

            // when
            handler.handle(event("TRANSFER", 1L, 2L, new BigDecimal("4000")));

            // then
            assertThat(source.getBalance()).isEqualByComparingTo(new BigDecimal("6000"));
            assertThat(target.getBalance()).isEqualByComparingTo(new BigDecimal("4000"));
        }
    }

    @Nested
    @DisplayName("계좌 미존재")
    class AccountNotFound {
        @Test
        @DisplayName("실패 - 계좌가 존재하지 않으면 예외 발생")
        void handle_whenAccountNotFound_throwsCustomException() {
            // given
            given(accountRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> handler.handle(event("INCOME", 99L, null, new BigDecimal("1000"))))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ACCOUNT_NOT_FOUND));
        }
    }
}
