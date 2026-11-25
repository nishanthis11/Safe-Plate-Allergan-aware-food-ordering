package com.example.smartfoodordering.dto.response;

import com.example.smartfoodordering.enums.OrderStatus;
import java.time.LocalDateTime;

public record StatusHistoryResponse(OrderStatus fromStatus, OrderStatus toStatus, String changedBy, String note,
                                    LocalDateTime at) {
}
