package com.rendos.payflow.transfers;

import com.rendos.payflow.accounts.entity.Account;
import com.rendos.payflow.accounts.entity.AccountStatus;
import com.rendos.payflow.accounts.entity.AccountType;
import com.rendos.payflow.accounts.repository.AccountRepository;
import com.rendos.payflow.common.exception.BusinessException;
import com.rendos.payflow.common.exception.InsufficientFundsException;
import com.rendos.payflow.transactions.repository.TransactionRepository;
import com.rendos.payflow.transfers.dto.TransferRequest;
import com.rendos.payflow.transfers.dto.TransferResponse;
import com.rendos.payflow.transfers.entity.Transfer;
import com.rendos.payflow.transfers.entity.TransferStatus;
import com.rendos.payflow.transfers.repository.TransferRepository;
import com.rendos.payflow.transfers.service.TransferService;
import com.rendos.payflow.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferService transferService;

    private User user;
    private Account accountA;
    private Account accountB;
    private UUID accountAId;
    private UUID accountBId;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("ros@example.com")
                .firstName("Ros")
                .lastName("Rendo")
                .build();

        accountAId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        accountBId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        accountA = Account.builder()
                .id(accountAId)
                .accountNumber("ACC-1000000001")
                .user(user)
                .accountType(AccountType.PERSONAL)
                .currency("USD")
                .balance(new BigDecimal("500.0000"))
                .status(AccountStatus.ACTIVE)
                .build();

        accountB = Account.builder()
                .id(accountBId)
                .accountNumber("ACC-1000000002")
                .user(user)
                .accountType(AccountType.BUSINESS)
                .currency("USD")
                .balance(new BigDecimal("300.0000"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should transfer $100 from Account A ($500) to Account B ($300) resulting in both having $400")
    void shouldTransferFundsSuccessfully() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(accountAId)
                .destinationAccountId(accountBId)
                .amount(new BigDecimal("100.0000"))
                .description("Test Transfer")
                .build();

        when(transferRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(accountRepository.findByIdWithPessimisticWriteLock(accountAId)).thenReturn(Optional.of(accountA));
        when(accountRepository.findByIdWithPessimisticWriteLock(accountBId)).thenReturn(Optional.of(accountB));

        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        // Act
        TransferResponse response = transferService.executeTransfer(request, "idempotency-token-123");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.0000"));

        // Balances updated
        assertThat(accountA.getBalance()).isEqualByComparingTo(new BigDecimal("400.0000"));
        assertThat(accountB.getBalance()).isEqualByComparingTo(new BigDecimal("400.0000"));

        // Verify ledger entries were saved (1 debit, 1 credit)
        verify(transactionRepository, times(2)).save(any());
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should reject transfer if sender has insufficient funds")
    void shouldThrowInsufficientFundsException() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(accountAId)
                .destinationAccountId(accountBId)
                .amount(new BigDecimal("1000.0000")) // Exceeds balance of $500
                .build();

        when(accountRepository.findByIdWithPessimisticWriteLock(accountAId)).thenReturn(Optional.of(accountA));
        when(accountRepository.findByIdWithPessimisticWriteLock(accountBId)).thenReturn(Optional.of(accountB));

        // Act & Assert
        assertThatThrownBy(() -> transferService.executeTransfer(request, null))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("insufficient balance");

        // Ensure balances untouched
        assertThat(accountA.getBalance()).isEqualByComparingTo(new BigDecimal("500.0000"));
        assertThat(accountB.getBalance()).isEqualByComparingTo(new BigDecimal("300.0000"));
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject transfer to the same account")
    void shouldRejectTransferToSameAccount() {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(accountAId)
                .destinationAccountId(accountAId)
                .amount(new BigDecimal("50.0000"))
                .build();

        assertThatThrownBy(() -> transferService.executeTransfer(request, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot transfer funds to the same account");
    }
}

