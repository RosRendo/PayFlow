package com.rendos.payflow.transactions.dto;

import com.rendos.payflow.transactions.entity.EntryType;
import com.rendos.payflow.transactions.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID id;
    private UUID transferId;
    private UUID accountId;
    private EntryType entryType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String currency;
    private String description;
    private Instant createdAt;

    public static TransactionResponse fromEntity(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .transferId(tx.getTransfer() != null ? tx.getTransfer().getId() : null)
                .accountId(tx.getAccount().getId())
                .entryType(tx.getEntryType())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .currency(tx.getCurrency())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}

