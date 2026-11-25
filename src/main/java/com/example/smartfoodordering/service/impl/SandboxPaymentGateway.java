package com.example.smartfoodordering.service.impl;

import com.example.smartfoodordering.dto.request.PaymentRequest;
import com.example.smartfoodordering.dto.response.PaymentResult;
import com.example.smartfoodordering.enums.PaymentMethod;
import com.example.smartfoodordering.enums.PaymentStatus;
import com.example.smartfoodordering.service.PaymentGateway;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Simulated gateway: no real money moves. Cash on delivery stays INITIATED until collected. */
@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "sandbox", matchIfMissing = true)
public class SandboxPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(PaymentRequest request) {
        if (request.method() == PaymentMethod.COD) {
            return new PaymentResult(PaymentStatus.INITIATED, reference(), "Pay on delivery");
        }
        if (request.simulateFailure()) {
            return new PaymentResult(PaymentStatus.FAILED, null, "Sandbox: payment declined");
        }
        return new PaymentResult(PaymentStatus.SUCCESS, reference(), "Sandbox: payment approved");
    }

    @Override
    public PaymentResult refund(String transactionReference, BigDecimal amount) {
        return new PaymentResult(PaymentStatus.REFUNDED, transactionReference, "Sandbox: refund processed");
    }

    @Override
    public String providerName() {
        return "sandbox";
    }

    private String reference() {
        return "SBX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
