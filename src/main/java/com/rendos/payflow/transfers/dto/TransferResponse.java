package com.rendos.payflow.transfers.dto;

import com.rendos.payflow.transfers.entity.Transfer;
import com.rendos.payflow.transfers.entity.TransferStatus;
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
public class TransferResponse {

    private UUID id;
    private String referenceId;
    private String idempotencyKey;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private TransferStatus status;
    private String description;
    private Instant createdAt;
    private Instant completedAt;

    public static TransferResponse fromEntity(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .referenceId(transfer.getReferenceId())
                .idempotencyKey(transfer.getIdempotencyKey())
                .sourceAccountId(transfer.getSourceAccount().getId())
                .destinationAccountId(transfer.getDestinationAccount().getId())
                .amount(transfer.getAmount())
                .currency(transfer.getCurrency())
                .status(transfer.getStatus())
                .description(transfer.getDescription())
                .createdAt(transfer.getCreatedAt())
                .completedAt(transfer.getCompletedAt())
                .build();
    }
}

