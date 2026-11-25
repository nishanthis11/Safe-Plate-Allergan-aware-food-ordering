package com.example.smartfoodordering.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PricingUtil {
    public static final BigDecimal TAX_RATE = new BigDecimal("0.05");

    private PricingUtil() {}

    public static BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal tax(BigDecimal subtotal) {
        return subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal total(BigDecimal subtotal, BigDecimal tax, BigDecimal deliveryFee) {
        return subtotal.add(tax).add(deliveryFee).setScale(2, RoundingMode.HALF_UP);
    }
}
