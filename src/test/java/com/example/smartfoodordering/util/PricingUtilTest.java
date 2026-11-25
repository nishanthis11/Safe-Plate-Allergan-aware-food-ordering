package com.example.smartfoodordering.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingUtilTest {

    @Test
    void lineTotalMultipliesAndRoundsToTwoPlaces() {
        assertThat(PricingUtil.lineTotal(new BigDecimal("240.00"), 3)).isEqualByComparingTo("720.00");
    }

    @Test
    void taxIsFivePercentRoundedHalfUp() {
        assertThat(PricingUtil.tax(new BigDecimal("420.00"))).isEqualByComparingTo("21.00");
        assertThat(PricingUtil.tax(new BigDecimal("33.30"))).isEqualByComparingTo("1.67");
    }

    @Test
    void totalAddsTaxAndDelivery() {
        BigDecimal subtotal = new BigDecimal("420.00");
        BigDecimal tax = PricingUtil.tax(subtotal);
        assertThat(PricingUtil.total(subtotal, tax, new BigDecimal("30.00"))).isEqualByComparingTo("471.00");
    }
}
