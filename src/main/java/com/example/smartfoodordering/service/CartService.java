package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.request.CartAddRequest;
import com.example.smartfoodordering.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(Long customerId);

    void addItem(Long customerId, CartAddRequest request);

    void updateQuantity(Long customerId, Long cartItemId, int quantity);

    void removeItem(Long customerId, Long cartItemId);

    void clear(Long customerId);
}
