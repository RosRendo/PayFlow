package com.rendos.payflow.accounts.controller;

import com.rendos.payflow.accounts.dto.AccountResponse;
import com.rendos.payflow.accounts.dto.CreateAccountRequest;
import com.rendos.payflow.accounts.service.AccountService;
import com.rendos.payflow.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account opening, balance inquiries, and statements")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Open a new financial account")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve account details and current balance")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(@PathVariable UUID id) {
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List all accounts for a specific user")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccountsByUserId(@PathVariable UUID userId) {
        List<AccountResponse> response = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

