package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.request.MenuItemRequest;
import com.example.smartfoodordering.dto.request.RestaurantRequest;
import com.example.smartfoodordering.dto.response.MenuItemResponse;
import com.example.smartfoodordering.dto.response.MenuItemView;
import com.example.smartfoodordering.dto.response.RestaurantResponse;
import com.example.smartfoodordering.security.CustomUserDetails;
import java.util.List;

public interface MenuService {
    // public / customer
    List<RestaurantResponse> listActiveRestaurants();

    RestaurantResponse getActiveRestaurant(Long restaurantId);

    /** customerId may be null (anonymous visitor): no safety warnings are computed then. */
    List<MenuItemView> getMenuForCustomer(Long restaurantId, Long customerId);

    // restaurant manager
    List<MenuItemResponse> getMenuForManager(CustomUserDetails actor);

    MenuItemRequest getFormForEdit(CustomUserDetails actor, Long itemId);

    Long save(CustomUserDetails actor, Long itemId, MenuItemRequest form);

    void setAvailability(CustomUserDetails actor, Long itemId, boolean available);

    // administrator
    List<RestaurantResponse> listAllRestaurants();

    RestaurantResponse createRestaurant(RestaurantRequest request, String actorEmail);

    void setRestaurantActive(Long restaurantId, boolean active, String actorEmail);
}
