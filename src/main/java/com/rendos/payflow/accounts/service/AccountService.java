package com.rendos.payflow.accounts.service;

import com.rendos.payflow.accounts.dto.AccountResponse;
import com.rendos.payflow.accounts.dto.CreateAccountRequest;
import com.rendos.payflow.accounts.entity.Account;
import com.rendos.payflow.accounts.entity.AccountStatus;
import com.rendos.payflow.accounts.repository.AccountRepository;
import com.rendos.payflow.common.exception.ResourceNotFoundException;
import com.rendos.payflow.common.util.AccountNumberGenerator;
import com.rendos.payflow.common.util.MoneyUtils;
import com.rendos.payflow.users.entity.User;
import com.rendos.payflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        String accountNumber;
        do {
            accountNumber = AccountNumberGenerator.generate();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        BigDecimal initialBalance = MoneyUtils.format(
                request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO
        );

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .user(user)
                .accountType(request.getAccountType())
                .currency(request.getCurrency().toUpperCase())
                .balance(initialBalance)
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        log.info("Opened account: id={}, accNo={}, balance={}", saved.getId(), saved.getAccountNumber(), saved.getBalance());
        return AccountResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        return AccountResponse.fromEntity(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(UUID userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(AccountResponse::fromEntity)
                .toList();
    }
}

