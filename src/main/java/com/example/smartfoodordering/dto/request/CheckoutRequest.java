package com.example.smartfoodordering.dto.request;

import com.example.smartfoodordering.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {
    @NotNull
    private Long addressId;

    @NotNull
    private PaymentMethod paymentMethod = PaymentMethod.UPI;

    private boolean acknowledgeWarnings;

    /** Sandbox only: forces the simulated gateway to decline the payment. */
    private boolean simulatePaymentFailure;
}
