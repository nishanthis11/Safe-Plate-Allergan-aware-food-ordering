package com.example.smartfoodordering.dto.response;

import com.example.smartfoodordering.enums.DietType;

public record DietPrefResponse(Long id, DietType dietType, boolean strict) {
}
