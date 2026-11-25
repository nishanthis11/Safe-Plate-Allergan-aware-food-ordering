package com.example.smartfoodordering.exception;

/** Violation of a domain rule (single-restaurant cart, invalid status transition, etc.). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
