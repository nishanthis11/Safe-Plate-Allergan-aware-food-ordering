package com.example.smartfoodordering.dto.response;

import com.example.smartfoodordering.enums.AllergySeverity;
import com.example.smartfoodordering.enums.WarningType;

public record WarningResponse(Long menuItemId, String itemName, WarningType type, String subject,
                              AllergySeverity severity, String message, boolean blocking) {
}
