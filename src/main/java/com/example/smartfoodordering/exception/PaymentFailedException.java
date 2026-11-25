package com.example.smartfoodordering.exception;

public class PaymentFailedException extends BusinessRuleException {
    public PaymentFailedException(String message) { super(message); }
}
