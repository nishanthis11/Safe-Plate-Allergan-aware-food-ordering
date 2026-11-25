package com.example.smartfoodordering.dto.response;

import java.util.List;

/** A menu item together with the safety warnings computed for the current customer. */
public record MenuItemView(MenuItemResponse item, List<WarningResponse> warnings) {

    public boolean blocked() {
        return warnings.stream().anyMatch(WarningResponse::blocking);
    }

    public boolean safe() {
        return warnings.isEmpty();
    }

    public String warningSummary() {
        return String.join(" ", warnings.stream().map(WarningResponse::message).toList());
    }
}
