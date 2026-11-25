package com.example.smartfoodordering.dto.response;

import com.example.smartfoodordering.enums.AllergySeverity;

public record AllergyResponse(Long id, String allergenCode, String allergenName, AllergySeverity severity, String notes) {
}
