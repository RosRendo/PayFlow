package com.rendos.payflow.transfers.controller;

import com.rendos.payflow.common.dto.ApiResponse;
import com.rendos.payflow.transfers.dto.TransferRequest;
import com.rendos.payflow.transfers.dto.TransferResponse;
import com.rendos.payflow.transfers.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "ACID money transfers between accounts with idempotency protection")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Transfer money between accounts", description = "Executes an atomic transfer, updates balances, and creates double-entry ledger records")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            @Parameter(description = "Optional unique idempotency token to prevent double charging on retry")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        TransferResponse response = transferService.executeTransfer(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transfer executed successfully", response));
    }
}

