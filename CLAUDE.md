# fintracking-account

계좌 관리, AES-256 계좌번호 암호화

---

## 패턴: Factory + Strategy

```
AccountFactory — 계좌 타입별 생성 (CHECKING / SAVINGS / ...)
EncryptionStrategy — AES256 / RSA 런타임 스위칭
  └── AES256EncryptionStrategy
```

---

## AES-256 암호화

- **대상 필드**: 계좌번호, 카드번호
- **방식**: AES-256 CBC + Random IV (레코드별 고유 IV)
- **키 위치**: Config 서버 `fintracking-account-secret.yml` → `encryption.aes-key`
- **적용**: `@Convert(converter = AesEncryptConverter.class)` 어노테이션

```java
// Entity 예시
@Convert(converter = AesEncryptConverter.class)
private String accountNumber;
```

- 응답 DTO에서 마스킹 처리 (앞 8자리만 노출 등)
- 키는 `@Value("${encryption.aes-key}")` 로만 주입, 하드코딩 절대 금지

---

## Kafka Consumer

`BalanceUpdateEventHandler` — `transaction.created` 토픽 구독

```
TransactionCreatedEvent 수신
  ├── INCOME  → account.deposit(amount)
  ├── EXPENSE → account.withdraw(amount)
  └── TRANSFER → from.withdraw(amount) + to.deposit(amount)
```

- `groupId: account-service`
- `@Transactional` 적용 (잔액 업데이트 원자성 보장)
- 이 서비스는 balance 관련 비즈니스 로직의 **단일 책임 서비스**

---

## 패키지 구조

```
com.ft.account
  ├── domain/          — Account 엔티티, 잔액 로직
  ├── application/     — AccountService, BalanceUpdateEventHandler
  ├── infrastructure/  — JPA, AesEncryptConverter
  └── presentation/    — AccountController, DTO
```

---

## 주요 ErrorCode

```java
ACCOUNT_NOT_FOUND(404, "ACCOUNT_001", "Account not found")
INSUFFICIENT_BALANCE(400, "ACCOUNT_002", "Insufficient balance")
DUPLICATE_ACCOUNT(409, "ACCOUNT_003", "Account already exists")
```
