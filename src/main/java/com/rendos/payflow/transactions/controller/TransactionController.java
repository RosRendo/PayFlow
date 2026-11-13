package com.rendos.payflow.transactions.controller;

import com.rendos.payflow.common.dto.ApiResponse;
import com.rendos.payflow.transactions.dto.TransactionResponse;
import com.rendos.payflow.transactions.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Ledger Transactions", description = "Audit trail and statement history")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get immutable ledger entries for an account")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByAccountId(@PathVariable UUID accountId) {
        List<TransactionResponse> response = transactionService.getTransactionsByAccountId(accountId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

