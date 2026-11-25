package com.example.smartfoodordering.dto.response;

import com.example.smartfoodordering.enums.OrderStatus;
import com.example.smartfoodordering.enums.PaymentMethod;
import com.example.smartfoodordering.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(Long id, String orderNumber, String restaurantName, String customerName,
                                  OrderStatus status, List<OrderItemResponse> items, List<WarningResponse> warnings,
                                  BigDecimal subtotal, BigDecimal tax, BigDecimal deliveryFee, BigDecimal total,
                                  String deliveryAddress, String deliveryPhone, LocalDateTime createdAt,
                                  PaymentStatus paymentStatus, PaymentMethod paymentMethod,
                                  List<StatusHistoryResponse> history, List<OrderStatus> nextStatuses,
                                  boolean cancellable, boolean warningsAcknowledged) {
}
