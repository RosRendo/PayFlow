package com.rendos.payflow.transactions.service;

import com.rendos.payflow.transactions.dto.TransactionResponse;
import com.rendos.payflow.transactions.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByAccountId(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByAccountIdPaged(UUID accountId, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, pageable)
                .map(TransactionResponse::fromEntity);
    }
}

