# fintracking-account

> 계좌 관리 · AES-256 계좌번호 암호화 · 잔액 업데이트

---

## 사용 기술

|    분류     |                 기술                 |
| :---------: | :----------------------------------: |
|   암호화    | AES-256 CBC (JPA AttributeConverter) |
|   메시지    |       Apache Kafka (Consumer)        |
|  설정 관리  |         Spring Cloud Config          |
| 서비스 등록 |      Spring Cloud Eureka Client      |

---

## 핵심 설계 패턴

### 1. Factory 패턴 — 계좌 생성

계좌 객체를 외부에서 `new Account(...)` 로 직접 생성하지 않는다.  
`AccountFactory` 를 통해 계좌 타입별 생성 로직을 캡슐화한다.

```
AccountFactory
  ├── createChecking()   → 입출금 계좌
  ├── createSavings()    → 적금 계좌
  ├── createInvestment() → 투자 계좌
  └── createCard()       → 카드 계좌
```

**왜 Factory가 필요한가?**
계좌 타입마다 초기 설정이 다를 수 있다(초기 잔액, 제한 한도 등).
타입별 생성 로직이 여러 곳에 흩어지면 일관성이 깨진다.
Factory 한 곳에서 관리하면 타입이 추가되어도 변경 지점이 하나다.

### 2. Strategy 패턴 — 암호화 방식 교체

```
EncryptionStrategy (인터페이스)
  └── AesEncryptionStrategy   ← 현재 사용 (AES-256)
  └── (RsaEncryptionStrategy) ← 미래 확장 가능
```

`EncryptionStrategy` 인터페이스를 두었기 때문에,
런타임에 암호화 방식을 바꾸거나 테스트 시 Mock으로 교체 용이

---

## AES-256 암호화 구조

### 어떻게 동작하는가?

계좌번호 필드에 `@Convert` 어노테이션 하나만 달면,
JPA가 자동으로 저장 시 암호화 / 조회 시 복호화 처리를 한다.

```java
// Account.java (도메인 엔티티)
@Convert(converter = AesEncryptConverter.class)
private String accountNumber;  // 메모리에선 평문, DB에선 암호화
```

```
[애플리케이션]                     [데이터베이스]
accountNumber = "1234-5678-9012"
        │
        │  저장 시 (AesEncryptConverter.convertToDatabaseColumn)
        ▼
암호화 → "U2Fsd29yZCBkYXRh..."  ──────────────► DB 저장
        │
        │  조회 시 (AesEncryptConverter.convertToEntityAttribute)
        ▼
복호화 ← "1234-5678-9012"       ◄─────────────  DB 조회
```

### 암호화 방식 상세

- **알고리즘**: AES-256 CBC
- **IV(초기화 벡터)**: 레코드마다 랜덤 생성 → 같은 계좌번호도 DB에서는 다른 값으로 저장됨
- **키 위치**: Config 서버 `fintracking-account-secret.yml` → `encryption.aes-key`
- **하드코딩 절대 금지**: 코드나 application.yml에서 기재 x

---

## Kafka 이벤트 처리

### BalanceUpdateEventHandler — 잔액 업데이트

`transaction-service`가 거래를 생성하면 Kafka로 이벤트를 발행한다.
`account-service`는 해당 이벤트를 받아 잔액을 업데이트한다.

```
[transaction-service]
    거래 생성
        │
        │ Kafka 발행 (topic: transaction.created)
        ▼
[account-service - BalanceUpdateEventHandler]
        │
        │ TransactionCreatedEvent 수신
        │
        ├── type = INCOME   → 해당 계좌 deposit(amount)
        ├── type = EXPENSE  → 해당 계좌 withdraw(amount)
        └── type = TRANSFER → from 계좌 withdraw(amount)
                              to   계좌 deposit(amount)
```

**중요한 점:**

- `@Transactional` 적용 → 입금/출금이 원자적으로 처리됨
- 소유자 검증 `validateOwner()`을 통하여 타인의 계좌 접근 제한
- 잔액 부족 시 `ACCOUNT_INSUFFICIENT_BALANCE` 예외 발생

---

## 도메인 모델 — Account

```java
Account
├── id              (Long)        ← PK
├── userId          (Long)        ← 소유자 ID (Gateway X-User-Id 헤더)
├── accountName     (String)      ← 계좌명
├── accountNumber   (String)      ← 계좌번호 (DB 적재 시 AES-256 자동 암호화)
├── accountType     (AccountType) ← CHECKING / SAVINGS / INVESTMENT / CARD
└── balance         (BigDecimal)  ← 잔액 (정밀도 손실 방지용 BigDecimal)

주요 메서드:
├── create()           ← 팩토리 메서드 (static, 검증 포함)
├── validateOwner()    ← 소유자 검증 (불일치 시 예외)
├── deposit(amount)    ← 입금 (0 이하 금액 거부)
├── withdraw(amount)   ← 출금 (0 이하 금액 또는 잔액 부족 시 거부)
└── updateName()       ← 계좌명 변경
```

---

## 패키지 구조

```
com.ft.account
├── domain/
│   ├── Account.java         ← 계좌 엔티티 + 비즈니스 로직
│   └── AccountType.java     ← CHECKING / SAVINGS / INVESTMENT / CARD
│
├── application/
│   ├── AccountService.java              ← 계좌 CRUD 유스케이스
│   ├── BalanceUpdateEventHandler.java   ← Kafka 이벤트 처리
│   ├── factory/
│   │   └── AccountFactory.java          ← Factory 패턴: 계좌 생성
│   ├── port/
│   │   ├── AccountRepository.java       ← DB 접근 추상화
│   │   └── EncryptionStrategy.java      ← Strategy 패턴: 암호화 인터페이스
│   └── dto/
│       ├── CreateAccountCommand.java
│       └── AccountResult.java
│
├── infrastructure/
│   ├── encryption/
│   │   ├── AesEncryptConverter.java     ← JPA AttributeConverter
│   │   ├── AesEncryptionStrategy.java   ← Strategy 구현체
│   │   └── AesEncryptor.java            ← AES-256 CBC 암복호화 로직
│   └── persistence/
│       ├── JpaAccountRepository.java
│       └── AccountRepositoryImpl.java
│
└── presentation/
    ├── AccountController.java
    └── dto/
        ├── CreateAccountRequest.java
        └── AccountResponse.java         ← 계좌번호 마스킹 처리
```

---

## API 엔드포인트

| 메서드 | 경로                                    | 설명              |
| ------ | --------------------------------------- | ----------------- |
| GET    | `/account-service/api/v1/accounts`      | 내 계좌 목록 조회 |
| POST   | `/account-service/api/v1/accounts`      | 계좌 등록         |
| GET    | `/account-service/api/v1/accounts/{id}` | 계좌 단건 조회    |
| DELETE | `/account-service/api/v1/accounts/{id}` | 계좌 삭제         |

> 모든 요청에 Gateway가 `X-User-Id` 헤더를 주입합니다. 컨트롤러는 이 값으로 소유자를 확인

---

## 에러 코드

| 코드                           | HTTP | 설명                |
| ------------------------------ | ---- | ------------------- |
| `ACCOUNT_NOT_FOUND`            | 404  | 계좌를 찾을 수 없음 |
| `ACCOUNT_NO_ACCESS`            | 403  | 접근 권한 없음      |
| `ACCOUNT_INSUFFICIENT_BALANCE` | 400  | 잔액 부족           |
| `ACCOUNT_INVALID_AMOUNT`       | 400  | 금액이 0 이하       |
| `ACCOUNT_OWNER_MISMATCH`       | 403  | 계좌 소유자 불일치  |

---

## 테스트

```
test/
├── domain/
│   └── AccountTest.java                 ← 입출금, 소유자 검증 단위 테스트
├── application/
│   ├── AccountServiceTest.java          ← 유스케이스 (Mockito)
│   └── BalanceUpdateEventHandlerTest.java ← Kafka 이벤트 처리 단위 테스트
└── infrastructure/
    └── JpaAccountRepositoryTest.java    ← @DataJpaTest + H2
```
