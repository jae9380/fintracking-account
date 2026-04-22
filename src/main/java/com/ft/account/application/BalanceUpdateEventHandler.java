package com.ft.account.application;

import com.ft.account.application.port.AccountRepository;
import com.ft.account.domain.Account;
import com.ft.common.event.TransactionCreatedEvent;
import com.ft.common.exception.CustomException;
import com.ft.common.kafka.EventHandler;
import com.ft.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.ft.common.exception.ErrorCode.ACCOUNT_NOT_FOUND;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceUpdateEventHandler implements EventHandler<TransactionCreatedEvent> {

    private final AccountRepository accountRepository;

    @KafkaListener(topics = KafkaTopic.TRANSACTION_CREATED, groupId = "account-service")
    @Transactional
    @Override
    public void handle(TransactionCreatedEvent event) {
        log.info("[BalanceUpdate] 이벤트 수신 — transactionId={}, type={}, accountId={}, amount={}",
                event.transactionId(), event.type(), event.accountId(), event.amount());

        switch (event.type()) {
            case "INCOME" -> getAccount(event.accountId()).deposit(event.amount());
            case "EXPENSE" -> getAccount(event.accountId()).withdraw(event.amount());
            case "TRANSFER" -> {
                getAccount(event.accountId()).withdraw(event.amount());
                if (event.toAccountId() != null) {
                    getAccount(event.toAccountId()).deposit(event.amount());
                }
            }
            default -> log.warn("[BalanceUpdate] 알 수 없는 거래 유형 — type={}", event.type());
        }
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ACCOUNT_NOT_FOUND));
    }
}
