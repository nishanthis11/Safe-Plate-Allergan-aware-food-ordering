package com.example.smartfoodordering.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStats(BigDecimal revenue, long totalOrders, long activeOrders,
                             List<String> statusLabels, List<Long> statusCounts,
                             List<String> dayLabels, List<BigDecimal> dayRevenue,
                             List<String> topItemNames, List<Long> topItemQuantities,
                             List<String> warningLabels, List<Long> warningCounts) {

    public long totalWarnings() {
        return warningCounts.stream().mapToLong(Long::longValue).sum();
    }
}
