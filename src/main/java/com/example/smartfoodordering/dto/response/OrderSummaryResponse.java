package com.example.smartfoodordering.dto.response;

import com.example.smartfoodordering.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(Long id, String orderNumber, String restaurantName, String customerName,
                                   OrderStatus status, BigDecimal total, LocalDateTime createdAt, int warningCount) {
}
