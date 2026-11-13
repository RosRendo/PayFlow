package com.rendos.payflow.transfers.service;

import com.rendos.payflow.accounts.entity.Account;
import com.rendos.payflow.accounts.repository.AccountRepository;
import com.rendos.payflow.common.exception.AccountFrozenException;
import com.rendos.payflow.common.exception.BusinessException;
import com.rendos.payflow.common.exception.InsufficientFundsException;
import com.rendos.payflow.common.exception.ResourceNotFoundException;
import com.rendos.payflow.common.util.AccountNumberGenerator;
import com.rendos.payflow.common.util.MoneyUtils;
import com.rendos.payflow.transactions.entity.EntryType;
import com.rendos.payflow.transactions.entity.Transaction;
import com.rendos.payflow.transactions.repository.TransactionRepository;
import com.rendos.payflow.transfers.dto.TransferRequest;
import com.rendos.payflow.transfers.dto.TransferResponse;
import com.rendos.payflow.transfers.entity.Transfer;
import com.rendos.payflow.transfers.entity.TransferStatus;
import com.rendos.payflow.transfers.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Executes an ACID financial transfer between two accounts with:
     * 1. Idempotency verification
     * 2. Global lock order sorting (deadlock-free pessimistic locking)
     * 3. Balance sufficiency verification
     * 4. Double-entry ledger entry insertion (DEBIT / CREDIT)
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public TransferResponse executeTransfer(TransferRequest request, String idempotencyKey) {
        log.info("Initiating transfer: from={} to={} amount={}",
                request.getSourceAccountId(), request.getDestinationAccountId(), request.getAmount());

        // 1. Idempotency Check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Transfer> existing = transferRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent hit for key: {}. Returning existing transfer reference: {}",
                        idempotencyKey, existing.get().getReferenceId());
                return TransferResponse.fromEntity(existing.get());
            }
        }

        // 2. Self-transfer check
        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            throw new BusinessException("Cannot transfer funds to the same account", "INVALID_DESTINATION", HttpStatus.BAD_REQUEST);
        }

        BigDecimal transferAmount = MoneyUtils.format(request.getAmount());
        if (!MoneyUtils.isPositive(transferAmount)) {
            throw new BusinessException("Transfer amount must be strictly greater than zero", "INVALID_AMOUNT", HttpStatus.BAD_REQUEST);
        }

        // 3. Deadlock-free account locking (Sort IDs so all threads lock in identical order)
        UUID sourceId = request.getSourceAccountId();
        UUID destId = request.getDestinationAccountId();

        UUID firstLockId = sourceId.compareTo(destId) < 0 ? sourceId : destId;
        UUID secondLockId = sourceId.compareTo(destId) < 0 ? destId : sourceId;

        Account firstLocked = accountRepository.findByIdWithPessimisticWriteLock(firstLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", firstLockId));
        Account secondLocked = accountRepository.findByIdWithPessimisticWriteLock(secondLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", secondLockId));

        Account sourceAccount = firstLockId.equals(sourceId) ? firstLocked : secondLocked;
        Account destAccount = firstLockId.equals(destId) ? firstLocked : secondLocked;

        // 4. Validate Account Status
        if (!sourceAccount.isActive()) {
            throw new AccountFrozenException(sourceAccount.getId(), sourceAccount.getStatus().name());
        }
        if (!destAccount.isActive()) {
            throw new AccountFrozenException(destAccount.getId(), destAccount.getStatus().name());
        }

        // 5. Currency Check
        if (!sourceAccount.getCurrency().equalsIgnoreCase(destAccount.getCurrency())) {
            throw new BusinessException(
                    String.format("Cross-currency transfer not supported in V1. Source: %s, Destination: %s",
                            sourceAccount.getCurrency(), destAccount.getCurrency()),
                    "CURRENCY_MISMATCH",
                    HttpStatus.BAD_REQUEST
            );
        }

        // 6. Check Balance
        if (sourceAccount.getBalance().compareTo(transferAmount) < 0) {
            throw new InsufficientFundsException(sourceAccount.getId(), transferAmount, sourceAccount.getBalance());
        }

        // 7. Mutate Balances
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(transferAmount));
        destAccount.setBalance(destAccount.getBalance().add(transferAmount));

        accountRepository.save(sourceAccount);
        accountRepository.save(destAccount);

        // 8. Record Transfer Entity
        String referenceId = AccountNumberGenerator.generateReference("TRF");
        Transfer transfer = Transfer.builder()
                .referenceId(referenceId)
                .idempotencyKey(idempotencyKey)
                .sourceAccount(sourceAccount)
                .destinationAccount(destAccount)
                .amount(transferAmount)
                .currency(sourceAccount.getCurrency())
                .status(TransferStatus.COMPLETED)
                .description(request.getDescription())
                .completedAt(Instant.now())
                .build();

        Transfer savedTransfer = transferRepository.save(transfer);

        // 9. Write Double-Entry Immutable Ledger
        Transaction debitTx = Transaction.builder()
                .transfer(savedTransfer)
                .account(sourceAccount)
                .entryType(EntryType.DEBIT)
                .amount(transferAmount)
                .balanceAfter(sourceAccount.getBalance())
                .currency(sourceAccount.getCurrency())
                .description("Transfer to " + destAccount.getAccountNumber() + (request.getDescription() != null ? " - " + request.getDescription() : ""))
                .build();

        Transaction creditTx = Transaction.builder()
                .transfer(savedTransfer)
                .account(destAccount)
                .entryType(EntryType.CREDIT)
                .amount(transferAmount)
                .balanceAfter(destAccount.getBalance())
                .currency(destAccount.getCurrency())
                .description("Transfer from " + sourceAccount.getAccountNumber() + (request.getDescription() != null ? " - " + request.getDescription() : ""))
                .build();

        transactionRepository.save(debitTx);
        transactionRepository.save(creditTx);

        log.info("Transfer completed successfully. ref={}, fromAcc={}, toAcc={}, amount={}",
                referenceId, sourceAccount.getAccountNumber(), destAccount.getAccountNumber(), transferAmount);

        return TransferResponse.fromEntity(savedTransfer);
    }
}

