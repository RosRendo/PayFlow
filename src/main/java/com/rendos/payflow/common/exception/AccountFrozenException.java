package com.rendos.payflow.common.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AccountFrozenException extends BusinessException {

    public AccountFrozenException(UUID accountId, String status) {
        super(
                String.format("Account %s is not active (current status: %s). Operations are restricted.", accountId, status),
                "ACCOUNT_NOT_ACTIVE",
                HttpStatus.FORBIDDEN
        );
    }
}

