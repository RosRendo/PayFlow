package com.rendos.payflow.accounts.dto;

import com.rendos.payflow.accounts.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g. USD, EUR, KHR)")
    @Builder.Default
    private String currency = "USD";

    @DecimalMin(value = "0.0", inclusive = true, message = "Initial deposit cannot be negative")
    @Builder.Default
    private BigDecimal initialDeposit = BigDecimal.ZERO;
}

