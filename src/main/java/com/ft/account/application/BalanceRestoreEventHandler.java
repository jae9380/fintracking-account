package com.ft.account.application;

import com.ft.account.application.port.AccountRepository;
import com.ft.account.domain.Account;
import com.ft.common.event.TransactionDeletedEvent;
import com.ft.common.exception.CustomException;
import com.ft.common.kafka.EventHandler;
import com.ft.common.kafka.KafkaTopic;
import com.ft.common.metric.annotation.MonitoredKafka;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.ft.common.exception.ErrorCode.ACCOUNT_NOT_FOUND;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceRestoreEventHandler implements EventHandler<TransactionDeletedEvent> {

    private final AccountRepository accountRepository;

    @MonitoredKafka(topic = KafkaTopic.TRANSACTION_DELETED, action = "consume")
    @KafkaListener(topics = KafkaTopic.TRANSACTION_DELETED, groupId = "account-service")
    @Transactional
    @Override
    public void handle(TransactionDeletedEvent event) {
        log.info("[BalanceRestore] 이벤트 수신 — transactionId={}, type={}, accountId={}, amount={}",
                event.transactionId(), event.type(), event.accountId(), event.amount());

        switch (event.type()) {
            case "INCOME" -> {
                Account account = getAccount(event.accountId());
                account.validateOwner(event.userId());
                account.withdraw(event.amount());
            }
            case "EXPENSE" -> {
                Account account = getAccount(event.accountId());
                account.validateOwner(event.userId());
                account.deposit(event.amount());
            }
            case "TRANSFER" -> {
                Account from = getAccount(event.accountId());
                from.validateOwner(event.userId());
                from.deposit(event.amount());
                if (event.toAccountId() != null) {
                    getAccount(event.toAccountId()).withdraw(event.amount());
                }
            }
            default -> log.warn("[BalanceRestore] 알 수 없는 거래 유형 — type={}", event.type());
        }
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ACCOUNT_NOT_FOUND));
    }
}
