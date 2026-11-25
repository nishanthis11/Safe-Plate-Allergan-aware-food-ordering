package com.example.smartfoodordering.dto.response;

import com.example.smartfoodordering.enums.PaymentStatus;

public record PaymentResult(PaymentStatus status, String reference, String message) {
    public boolean success() {
        return status != PaymentStatus.FAILED;
    }
}
