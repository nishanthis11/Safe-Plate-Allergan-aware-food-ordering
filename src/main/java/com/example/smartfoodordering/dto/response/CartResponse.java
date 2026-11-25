package com.example.smartfoodordering.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long restaurantId, String restaurantName, List<CartItemResponse> items,
                           BigDecimal subtotal, BigDecimal tax, BigDecimal deliveryFee, BigDecimal total,
                           AllergenCheckResult check) {

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
