package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.request.CheckoutRequest;
import com.example.smartfoodordering.dto.response.OrderDetailResponse;
import com.example.smartfoodordering.dto.response.OrderSummaryResponse;
import com.example.smartfoodordering.enums.OrderStatus;
import com.example.smartfoodordering.security.CustomUserDetails;
import java.time.Duration;
import java.util.List;

public interface OrderService {
    // customer
    OrderDetailResponse placeOrder(Long customerId, String customerEmail, CheckoutRequest request);

    List<OrderSummaryResponse> listForCustomer(Long customerId);

    OrderDetailResponse getForCustomer(Long customerId, Long orderId);

    void cancelByCustomer(Long customerId, String customerEmail, Long orderId, String reason);

    // restaurant staff (manager / kitchen) and admin
    List<OrderSummaryResponse> listForStaff(CustomUserDetails actor);

    List<OrderSummaryResponse> kitchenQueue(CustomUserDetails actor);

    OrderDetailResponse getForStaff(CustomUserDetails actor, Long orderId);

    OrderDetailResponse updateStatus(CustomUserDetails actor, Long orderId, OrderStatus next, String note);

    // scheduler
    int cancelStalePendingOrders(Duration olderThan);
}
