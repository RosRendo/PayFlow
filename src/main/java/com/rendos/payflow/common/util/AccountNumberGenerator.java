package com.rendos.payflow.common.util;

import java.security.SecureRandom;

public final class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private AccountNumberGenerator() {}

    public static String generate() {
        // Generates an enterprise-standard formatted account string: ACC-XXXXXXXXXX
        long num = 100_000_0000L + (long)(RANDOM.nextDouble() * 900_000_0000L);
        return "ACC-" + num;
    }

    public static String generateReference(String prefix) {
        long num = 100_000L + (long)(RANDOM.nextDouble() * 900_000L);
        return prefix + "-" + System.currentTimeMillis() + "-" + num;
    }
}

