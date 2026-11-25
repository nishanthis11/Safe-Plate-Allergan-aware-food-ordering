package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.response.RecommendationResponse;
import java.util.List;

public interface RecommendationService {
    List<RecommendationResponse> recommend(Long customerId);
}
