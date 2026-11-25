package com.example.smartfoodordering.dto.request;

import com.example.smartfoodordering.enums.PaymentMethod;
import java.math.BigDecimal;

public record PaymentRequest(String orderNumber, BigDecimal amount, PaymentMethod method, boolean simulateFailure) {
}
