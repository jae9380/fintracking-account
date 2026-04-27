package com.ft.account.domain;

import com.ft.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.ft.common.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Account 도메인 테스트")
class AccountTest {
    @Nested
    @DisplayName("계좌 생성")
    class Create {
        @Test
        @DisplayName("성공 - 초기 잔액은 0이다")
        void success_validInput_initialBalanceIsZero() {
            // given
            Long userId = 1L;
            String accountName = "내 통장";
            String accountNumber = "1234-5678-9012";
            AccountType accountType = AccountType.CHECKING;

            // when
            Account account = Account.create(userId, accountName, accountNumber, accountType);

            // then
            assertThat(account.getUserId()).isEqualTo(1L);
            assertThat(account.getAccountName()).isEqualTo("내 통장");
            assertThat(account.getAccountNumber()).isEqualTo("1234-5678-9012");
            assertThat(account.getAccountType()).isEqualTo(AccountType.CHECKING);
            assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("실패 - 계좌명이 공백이면 예외 발생")
        void fail_blankName_throwsException() {
            // given
            String blankName = "  ";

            // when & then
            assertThatThrownBy(() -> Account.create(1L, blankName, "1234-5678-9012", AccountType.CHECKING))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ACCOUNT_INVALID_NAME));
        }

        @Test
        @DisplayName("실패 - 계좌명이 null이면 예외 발생")
        void fail_nullName_throwsException() {
            // when & then
            assertThatThrownBy(() -> Account.create(1L, null, "1234-5678-9012", AccountType.CHECKING))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ACCOUNT_INVALID_NAME));
        }

        @Test
        @DisplayName("실패 - 계좌번호가 공백이면 예외 발생")
        void fail_blankAccountNumber_throwsException() {
            // given
            String blankNumber = "  ";

            // when & then
            assertThatThrownBy(() -> Account.create(1L, "내 통장", blankNumber, AccountType.CHECKING))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ACCOUNT_INVALID_NUMBER));
        }
    }

    @Nested
    @DisplayName("소유자 검증")
    class ValidateOwner {
        @Test
        @DisplayName("성공 - 본인이면 예외 없음")
        void success_sameUser_noException() {
            // given
            Long userId = 1L;
            Account account = Account.create(userId, "내 통장", "1234-5678-9012", AccountType.CHECKING);

            // when & then
            assertThatNoException().isThrownBy(() -> account.validateOwner(userId));
        }

        @Test
        @DisplayName("실패 - 다른 사용자면 예외 발생")
        void fail_differentUser_throwsException() {
            // given
            Account account = Account.create(1L, "내 통장", "1234-5678-9012", AccountType.CHECKING);

            // when & then
            assertThatThrownBy(() -> account.validateOwner(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ACCOUNT_OWNER_MISMATCH));
        }
    }

    @Nested
    @DisplayName("입금")
    class Deposit {
        @Test
        @DisplayName("성공 - 잔액이 증가한다")
        void success_positiveAmount_balanceIncreases() {
            // given
            Account account = Account.create(1L, "내 통장", "1234-5678-9012", AccountType.CHECKING);
            BigDecimal depositAmount = new BigDecimal("10000");

            // when
            account.deposit(depositAmount);

            // then
            assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("성공 - 여러 번 입금하면 누적된다")
        void success_multipleDeposits_accumulatesBalance() {
            // given
            Account account = Account.create(1L, "내 통장", "1234-5678-9012", AccountType.CHECKING);

            // when
            account.deposit(new BigDecimal("10000"));
            account.deposit(new BigDecimal("5000"));

            // then
            assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("15000"));
        }

        @Test
        @DisplayName("실패 - 0원이면 예외 발생")
        void fail_zeroAmount_throwsException() {
            // given
            Account account = Account.create(1L, "내 통장", "1234-5678-9012", AccountType.CHECKING);

            // when & then
            assertThatThrownBy(() -> account.deposit(BigDecimal.ZERO))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ACCOUNT_INVALID_AMOUNT));
        }

        @Test
        @DisplayName("실패 - 음수 금액이면 예외 발생")
        void fail_negativeAmount_throwsException() {
            // given
            Account account = Account.create(1L, "내 통장", "1234-5678-9012", AccountType.CHECKING);

            // when & then
            assertThatThrownBy(() -> account.deposit(new BigDecimal("-1000")))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ACCOUNT_INVALID_AMOUNT));
        }
    }

    @Nested
    @DisplayName("출금")
    class Withdraw {
        @Test
        @DisplayName("성공 - 잔액이 감소한다")
        void success_sufficientBalance_balanceDecreases() {
            // given
            Account account = Account.create(1L, "내 통장", "1234-5678-9012", AccountType.CHECKING);
            account.deposit(new BigDecimal("10000"));
            BigDecimal withdrawAmount = new BigDecimal("3000");

            // when
            account.withdraw(withdrawAmount);

            // then
            assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("7000"));
        }

        @Test
        @DisplayName("실패 - 잔액 부족이면 예외 발생")
        void fail_insufficientBalance_throwsException() {
            // given
            Account account = Account.create(1L, "내 통장", "1234-5678-9012", AccountType.CHECKING);
            account.deposit(new BigDecimal("1000"));

            // when & then
            assertThatThrownBy(() -> account.withdraw(new BigDecimal("5000")))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ACCOUNT_INSUFFICIENT_BALANCE));
        }

        @Test
        @DisplayName("실패 - 0원이면 예외 발생")
        void fail_zeroAmount_throwsException() {
            // given
            Account account = Account.create(1L, "내 통장", "1234-5678-9012", AccountType.CHECKING);
            account.deposit(new BigDecimal("10000"));

            // when & then
            assertThatThrownBy(() -> account.withdraw(BigDecimal.ZERO))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ACCOUNT_INVALID_AMOUNT));
        }
    }

    @Nested
    @DisplayName("계좌명 변경")
    class UpdateName {
        @Test
        @DisplayName("성공 - 계좌명이 변경된다")
        void success_validName_nameUpdated() {
            // given
            Account account = Account.create(1L, "내 통장", "1234-5678-9012", AccountType.CHECKING);
            String newName = "새 통장 이름";

            // when
            account.updateName(newName);

            // then
            assertThat(account.getAccountName()).isEqualTo("새 통장 이름");
        }

        @Test
        @DisplayName("실패 - 공백이면 예외 발생")
        void fail_blankName_throwsException() {
            // given
            Account account = Account.create(1L, "내 통장", "1234-5678-9012", AccountType.CHECKING);

            // when & then
            assertThatThrownBy(() -> account.updateName("  "))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ACCOUNT_INVALID_NAME));
        }
    }
}
