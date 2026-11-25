package com.example.smartfoodordering.dto.response;

import java.util.List;

public record RecommendationResponse(MenuItemResponse item, int score, List<String> reasons) {
}
