package com.example.smartfoodordering.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(Long cartItemId, Long menuItemId, String name, BigDecimal unitPrice, int quantity,
                               BigDecimal lineTotal, String specialInstructions) {
}
