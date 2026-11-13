# PayFlow Architecture & Technical Specification

## 1. System Overview

```
                      ┌──────────────────────┐
                      │    Client / Web      │
                      └──────────┬───────────┘
                                 │ HTTP / JSON
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot 3.3+ (Java 21)                  │
│                                                                 │
│  ┌────────────────┐   ┌─────────────────┐   ┌────────────────┐  │
│  │  Auth & JWT    │   │ Account Service │   │ Transfer Core  │  │
│  └────────────────┘   └─────────────────┘   └────────┬───────┘  │
│                                                      │          │
│  ┌────────────────┐   ┌─────────────────┐            │          │
│  │ Idempotency    │   │  Audit Logging  │            ▼          │
│  │ (Redis Cache)  │   │  (Async Event)  │   ┌────────────────┐  │
│  └────────────────┘   └─────────────────┘   │ Double-Entry   │  │
│                                             │ Ledger Engine  │  │
│                                             └────────┬───────┘  │
└──────────────────────────────────────────────────────┼──────────┘
                                                       │
                                 ┌─────────────────────┴──────┐
                                 ▼                            ▼
                      ┌──────────────────────┐   ┌────────────────┐
                      │    PostgreSQL 16     │   │    Redis 7     │
                      │  (ACID Transactions) │   │ (Idempotency)  │
                      └──────────────────────┘   └────────────────┘
```

---

## 2. Core Financial Rules & Invariants

1. **Balance Invariant**:
   $$\text{Balance}_{\text{Account}} \ge 0$$
   An account cannot be overdrawn unless an explicit credit facility is granted. Balances are validated both at the application service level and at the database level using a check constraint `CHECK (balance >= 0)`.

2. **The Double-Entry Equation**:
   For any completed transfer of amount $M$:
   $$\Delta \text{Balance}_{\text{Sender}} = -M \quad (\text{DEBIT})$$
   $$\Delta \text{Balance}_{\text{Recipient}} = +M \quad (\text{CREDIT})$$
   $$\sum \text{Ledger Entries} = 0$$

3. **Global Lock Acquisition Ordering (Deadlock Defense)**:
   When transfer occurs between Account $A$ and Account $B$:
   $$\text{Lock}_1 = \text{acquireLock}(\min(A.\text{id}, B.\text{id}))$$
   $$\text{Lock}_2 = \text{acquireLock}(\max(A.\text{id}, B.\text{id}))$$
   This strictly avoids PostgreSQL deadlock code `40P01` under parallel inverse transfers.

---

## 3. Database Schema DDL (PostgreSQL)

```sql
-- 1. Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Accounts
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number VARCHAR(32) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    account_type VARCHAR(50) NOT NULL, -- 'PERSONAL', 'BUSINESS', 'SAVINGS'
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'FROZEN', 'CLOSED'
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_positive_balance CHECK (balance >= 0)
);

-- 3. Transfers
CREATE TABLE transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id VARCHAR(64) UNIQUE NOT NULL,
    idempotency_key VARCHAR(128) UNIQUE,
    source_account_id UUID NOT NULL REFERENCES accounts(id),
    destination_account_id UUID NOT NULL REFERENCES accounts(id),
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'COMPLETED', 'FAILED', 'REVERSED'
    description VARCHAR(255),
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_different_accounts CHECK (source_account_id != destination_account_id),
    CONSTRAINT chk_positive_amount CHECK (amount > 0)
);

-- 4. Transactions (Double-Entry Ledger)
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_id UUID REFERENCES transfers(id) ON DELETE SET NULL,
    account_id UUID NOT NULL REFERENCES accounts(id),
    entry_type VARCHAR(10) NOT NULL, -- 'DEBIT', 'CREDIT'
    amount NUMERIC(19, 4) NOT NULL,
    balance_after NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Audit Logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    ip_address VARCHAR(45),
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for High Throughput
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_transfers_source ON transfers(source_account_id);
CREATE INDEX idx_transfers_destination ON transfers(destination_account_id);
CREATE INDEX idx_transactions_account_id_created ON transactions(account_id, created_at DESC);
CREATE INDEX idx_transfers_idempotency ON transfers(idempotency_key);
```

---

## 4. Folder Structure (Spring Boot Clean Modular Architecture)

```
payflow-backend/
├── src/
│   ├── main/
│   │   ├── java/com/rendos/payflow/
│   │   │   ├── PayflowApplication.java
│   │   │   │
│   │   │   ├── common/
│   │   │   │   ├── config/             # SecurityConfig, RedisConfig, OpenApiConfig
│   │   │   │   ├── dto/                # ApiResponse<T>, ErrorResponse
│   │   │   │   ├── exception/          # GlobalExceptionHandler, Custom Exceptions
│   │   │   │   └── util/               # MoneyUtils, AccountNumberGenerator
│   │   │   │
│   │   │   ├── auth/
│   │   │   │   ├── controller/         # AuthController (/api/v1/auth)
│   │   │   │   ├── dto/                # LoginRequest, RegisterRequest, AuthResponse
│   │   │   │   ├── jwt/                # JwtTokenProvider, JwtAuthenticationFilter
│   │   │   │   └── service/            # AuthService, CustomUserDetailsService
│   │   │   │
│   │   │   ├── users/
│   │   │   │   ├── entity/             # User.java
│   │   │   │   ├── repository/         # UserRepository.java
│   │   │   │   ├── service/            # UserService.java
│   │   │   │   └── controller/         # UserController.java
│   │   │   │
│   │   │   ├── accounts/
│   │   │   │   ├── entity/             # Account.java, AccountType, AccountStatus
│   │   │   │   ├── repository/         # AccountRepository.java (Pessimistic lock query)
│   │   │   │   ├── service/            # AccountService.java
│   │   │   │   ├── dto/                # CreateAccountDto, AccountResponseDto
│   │   │   │   └── controller/         # AccountController.java
│   │   │   │
│   │   │   ├── transfers/
│   │   │   │   ├── entity/             # Transfer.java, TransferStatus
│   │   │   │   ├── repository/         # TransferRepository.java
│   │   │   │   ├── service/            # TransferService.java (Core @Transactional engine)
│   │   │   │   ├── dto/                # TransferRequestDto, TransferResponseDto
│   │   │   │   └── controller/         # TransferController.java
│   │   │   │
│   │   │   ├── transactions/
│   │   │   │   ├── entity/             # Transaction.java (Ledger), EntryType (DEBIT/CREDIT)
│   │   │   │   ├── repository/         # TransactionRepository.java
│   │   │   │   ├── service/            # TransactionService.java
│   │   │   │   └── controller/         # TransactionController.java
│   │   │   │
│   │   │   └── audit/
│   │   │       ├── entity/             # AuditLog.java
│   │   │       ├── aspect/             # AuditLogAspect.java
│   │   │       └── repository/         # AuditLogRepository.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/
│   │           └── V1__init_schema.sql
│   │
│   └── test/
│       └── java/com/rendos/payflow/
│           ├── unit/                   # Unit tests
│           └── concurrency/            # Multi-threaded transfer race-condition tests
│
├── docker-compose.yml                  # PostgreSQL & Redis
├── Dockerfile
└── pom.xml
```

---

## 5. The First 10 Tasks Action Roadmap

| Task | Title | Description & Milestone |
| :--- | :--- | :--- |
| **01** | **JDK & Tooling Setup** | Install Java 21 (Eclipse Temurin) and verify Docker engine. |
| **02** | **Project Bootstrap** | Initialize Spring Boot 3.3+ via Spring Initializr with Web, JPA, Lombok, Validation, Flyway, PostgreSQL. |
| **03** | **Container Infrastructure** | Configure `docker-compose.yml` for PostgreSQL 16 & Redis 7, then test connection in `application.yml`. |
| **04** | **Flyway Baseline Migration** | Implement `V1__init_schema.sql` and verify that Spring Boot automatically generates the tables on startup. |
| **05** | **JPA Domain Entities** | Implement `User`, `Account`, `Transfer`, and `Transaction` with `BigDecimal`, Enums, and Auditing annotations. |
| **06** | **Account REST API** | Build `AccountController`, `AccountService`, and DTOs with validation and a `GlobalExceptionHandler`. |
| **07** | **Atomic Transfer Engine** | Implement `TransferService.transfer(...)` with `@Transactional`, debit/credit balance checks, and ledger recording. |
| **08** | **Deadlock-Free Locking** | Add `@Lock(LockModeType.PESSIMISTIC_WRITE)` and enforce ID-sorting so parallel transfers never deadlock. |
| **09** | **Stress Concurrency Testing** | Write JUnit 5 test with 50 threads transferring money concurrently to prove zero lost updates. |
| **10** | **Idempotency Defense** | Add Redis-backed `Idempotency-Key` interceptor to reject duplicate transfer calls gracefully. |

