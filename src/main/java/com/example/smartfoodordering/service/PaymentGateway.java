package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.request.PaymentRequest;
import com.example.smartfoodordering.dto.response.PaymentResult;
import java.math.BigDecimal;

/** Abstraction over a payment provider. Only a sandbox simulator is provided. */
public interface PaymentGateway {
    PaymentResult charge(PaymentRequest request);

    PaymentResult refund(String transactionReference, BigDecimal amount);

    String providerName();
}
