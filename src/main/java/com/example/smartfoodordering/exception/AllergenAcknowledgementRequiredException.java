package com.example.smartfoodordering.exception;

import com.example.smartfoodordering.dto.response.AllergenCheckResult;

/** Thrown at checkout when non-blocking warnings exist and the customer has not acknowledged them. */
public class AllergenAcknowledgementRequiredException extends RuntimeException {
    private final transient AllergenCheckResult result;

    public AllergenAcknowledgementRequiredException(AllergenCheckResult result) {
        super("Allergen or dietary warnings must be acknowledged before placing this order");
        this.result = result;
    }

    public AllergenCheckResult getResult() { return result; }
}
