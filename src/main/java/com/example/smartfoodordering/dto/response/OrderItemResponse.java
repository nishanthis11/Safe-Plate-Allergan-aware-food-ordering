package com.example.smartfoodordering.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(String name, BigDecimal unitPrice, int quantity, BigDecimal lineTotal,
                                String allergens, String specialInstructions) {
}
