package com.example.smartfoodordering.service.impl;

import com.example.smartfoodordering.dto.response.DashboardStats;
import com.example.smartfoodordering.entity.FoodOrder;
import com.example.smartfoodordering.enums.OrderStatus;
import com.example.smartfoodordering.repository.FoodOrderRepository;
import com.example.smartfoodordering.repository.OrderAllergenWarningRepository;
import com.example.smartfoodordering.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {
    private static final int DAYS = 7;
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd MMM");

    private final FoodOrderRepository orderRepository;
    private final OrderAllergenWarningRepository warningRepository;

    public ReportServiceImpl(FoodOrderRepository orderRepository, OrderAllergenWarningRepository warningRepository) {
        this.orderRepository = orderRepository;
        this.warningRepository = warningRepository;
    }

    @Override
    public DashboardStats statsFor(Long restaurantId) {
        List<String> statusLabels = new ArrayList<>();
        List<Long> statusCounts = new ArrayList<>();
        long total = 0;
        long active = 0;
        for (Object[] row : orderRepository.countByStatus(restaurantId)) {
            OrderStatus status = (OrderStatus) row[0];
            long count = ((Number) row[1]).longValue();
            statusLabels.add(status.name());
            statusCounts.add(count);
            total += count;
            if (!status.isTerminal()) {
                active += count;
            }
        }

        BigDecimal revenue = orderRepository.totalRevenue(restaurantId);
        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }

        LocalDate today = LocalDate.now();
        LocalDate first = today.minusDays(DAYS - 1L);
        List<FoodOrder> recent = restaurantId == null
                ? orderRepository.findByCreatedAtAfterAndStatusNot(first.atStartOfDay(), OrderStatus.CANCELLED)
                : orderRepository.findByRestaurantIdAndCreatedAtAfterAndStatusNot(restaurantId, first.atStartOfDay(),
                        OrderStatus.CANCELLED);
        Map<LocalDate, BigDecimal> perDay = new TreeMap<>();
        for (int i = 0; i < DAYS; i++) {
            perDay.put(first.plusDays(i), BigDecimal.ZERO);
        }
        for (FoodOrder o : recent) {
            perDay.merge(o.getCreatedAt().toLocalDate(), o.getTotal(), BigDecimal::add);
        }
        List<String> dayLabels = perDay.keySet().stream().map(d -> d.format(LABEL)).toList();
        List<BigDecimal> dayRevenue = new ArrayList<>(perDay.values());

        List<String> topNames = new ArrayList<>();
        List<Long> topQuantities = new ArrayList<>();
        for (Object[] row : orderRepository.topItems(restaurantId, PageRequest.of(0, 5))) {
            topNames.add((String) row[0]);
            topQuantities.add(((Number) row[1]).longValue());
        }

        List<String> warningLabels = new ArrayList<>();
        List<Long> warningCounts = new ArrayList<>();
        for (Object[] row : warningRepository.countByType(restaurantId)) {
            warningLabels.add(row[0].toString());
            warningCounts.add(((Number) row[1]).longValue());
        }

        return new DashboardStats(revenue, total, active, statusLabels, statusCounts, dayLabels, dayRevenue,
                topNames, topQuantities, warningLabels, warningCounts);
    }
}
