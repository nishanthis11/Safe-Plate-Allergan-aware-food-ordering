package com.example.smartfoodordering.mapper;

import com.example.smartfoodordering.dto.response.OrderDetailResponse;
import com.example.smartfoodordering.dto.response.OrderItemResponse;
import com.example.smartfoodordering.dto.response.OrderSummaryResponse;
import com.example.smartfoodordering.dto.response.StatusHistoryResponse;
import com.example.smartfoodordering.dto.response.WarningResponse;
import com.example.smartfoodordering.entity.FoodOrder;
import com.example.smartfoodordering.entity.Payment;
import com.example.smartfoodordering.enums.OrderStatus;
import com.example.smartfoodordering.enums.WarningType;
import java.util.List;
import org.springframework.stereotype.Component;

/** Must be called inside a transaction: it reads lazy associations. */
@Component
public class OrderMapper {

    public OrderSummaryResponse toSummary(FoodOrder o) {
        return new OrderSummaryResponse(o.getId(), o.getOrderNumber(), o.getRestaurant().getName(),
                o.getCustomer().getFullName(), o.getStatus(), o.getTotal(), o.getCreatedAt(), o.getWarnings().size());
    }

    public OrderDetailResponse toDetail(FoodOrder o, List<OrderStatus> nextStatuses) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderItemResponse(i.getItemNameSnapshot(), i.getUnitPriceSnapshot(), i.getQuantity(),
                        i.getLineTotal(), i.getAllergensSnapshot(), i.getSpecialInstructions()))
                .toList();
        List<WarningResponse> warnings = o.getWarnings().stream()
                .map(w -> new WarningResponse(null, w.getItemName(), w.getWarningType(), w.getSubject(),
                        w.getSeverity(), w.getMessage(), w.getWarningType() == WarningType.KNOWN_CONFLICT))
                .toList();
        List<StatusHistoryResponse> history = o.getHistory().stream()
                .map(h -> new StatusHistoryResponse(h.getFromStatus(), h.getToStatus(), h.getChangedByEmail(),
                        h.getNote(), h.getCreatedAt()))
                .toList();
        Payment p = o.getPayment();
        return new OrderDetailResponse(o.getId(), o.getOrderNumber(), o.getRestaurant().getName(),
                o.getCustomer().getFullName(), o.getStatus(), items, warnings, o.getSubtotal(), o.getTax(),
                o.getDeliveryFee(), o.getTotal(), o.getDeliveryAddressSnapshot(), o.getDeliveryPhoneSnapshot(),
                o.getCreatedAt(), p == null ? null : p.getStatus(), p == null ? null : p.getMethod(), history,
                nextStatuses, o.getStatus().canTransitionTo(OrderStatus.CANCELLED), o.isWarningsAcknowledged());
    }
}
