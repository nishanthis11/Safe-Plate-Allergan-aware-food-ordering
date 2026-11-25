package com.example.smartfoodordering.exception;

import com.example.smartfoodordering.dto.response.AllergenCheckResult;

/** Thrown when a KNOWN_CONFLICT blocks the order outright. */
public class AllergenConflictException extends RuntimeException {
    private final transient AllergenCheckResult result;

    public AllergenConflictException(AllergenCheckResult result) {
        super("Order blocked: one or more items contain an allergen from your food profile");
        this.result = result;
    }

    public AllergenCheckResult getResult() { return result; }
}
