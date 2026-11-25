package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.response.DashboardStats;

public interface ReportService {
    /** restaurantId null means platform-wide (administrator). */
    DashboardStats statsFor(Long restaurantId);
}
