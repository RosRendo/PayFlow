package com.rendos.payflow.common.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException(UUID accountId, BigDecimal requestedAmount, BigDecimal currentBalance) {
        super(
                String.format("Account %s has insufficient balance. Required: %s, Current: %s",
                        accountId, requestedAmount, currentBalance),
                "INSUFFICIENT_FUNDS",
                HttpStatus.UNPROCESSABLE_ENTITY
        );
    }
}

