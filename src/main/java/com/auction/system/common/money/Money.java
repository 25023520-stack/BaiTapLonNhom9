package com.auction.system.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private static final int DATABASE_SCALE = 2;

    private Money() {
    }

    public static double normalize(double amount) {
        return toDatabaseAmount(amount).doubleValue();
    }

    public static BigDecimal toDatabaseAmount(double amount) {
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("Money amount must be finite");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Money amount must not be negative");
        }
        return BigDecimal.valueOf(amount).setScale(DATABASE_SCALE, RoundingMode.HALF_UP);
    }
}
