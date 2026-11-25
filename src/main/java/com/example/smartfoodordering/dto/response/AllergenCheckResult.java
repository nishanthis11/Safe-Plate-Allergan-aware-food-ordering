package com.example.smartfoodordering.dto.response;

import java.util.List;

public record AllergenCheckResult(List<WarningResponse> warnings) {

    public static AllergenCheckResult empty() {
        return new AllergenCheckResult(List.of());
    }

    public boolean hasBlocking() {
        return warnings.stream().anyMatch(WarningResponse::blocking);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /** True when there are only non-blocking warnings, which the customer must acknowledge. */
    public boolean requiresAcknowledgement() {
        return !hasBlocking() && hasWarnings();
    }

    public List<WarningResponse> blockingWarnings() {
        return warnings.stream().filter(WarningResponse::blocking).toList();
    }

    public String summary() {
        return String.join(" | ", warnings.stream().map(w -> w.itemName() + ": " + w.message()).toList());
    }
}
