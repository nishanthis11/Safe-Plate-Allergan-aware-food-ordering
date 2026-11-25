package com.example.smartfoodordering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.smartfoodordering.dto.request.CartAddRequest;
import com.example.smartfoodordering.dto.response.AllergenCheckResult;
import com.example.smartfoodordering.dto.response.WarningResponse;
import com.example.smartfoodordering.entity.Cart;
import com.example.smartfoodordering.entity.CartItem;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.entity.Restaurant;
import com.example.smartfoodordering.enums.AllergySeverity;
import com.example.smartfoodordering.enums.WarningType;
import com.example.smartfoodordering.exception.AllergenConflictException;
import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.repository.CartRepository;
import com.example.smartfoodordering.repository.MenuItemRepository;
import com.example.smartfoodordering.repository.UserRepository;
import com.example.smartfoodordering.service.impl.CartServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    private static final Long CUSTOMER_ID = 1L;

    @Mock
    private CartRepository cartRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AllergenDetectionService detectionService;

    private CartServiceImpl service;
    private Restaurant restaurantA;
    private Restaurant restaurantB;

    @BeforeEach
    void setUp() {
        service = new CartServiceImpl(cartRepository, menuItemRepository, userRepository, detectionService);
        restaurantA = restaurant(1L, "A");
        restaurantB = restaurant(2L, "B");
    }

    private Restaurant restaurant(Long id, String name) {
        Restaurant r = new Restaurant();
        r.setId(id);
        r.setName(name);
        return r;
    }

    private MenuItem item(Long id, Restaurant restaurant) {
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setName("Dish " + id);
        m.setPrice(new BigDecimal("100.00"));
        m.setRestaurant(restaurant);
        return m;
    }

    private CartAddRequest request(Long menuItemId, int quantity) {
        CartAddRequest r = new CartAddRequest();
        r.setMenuItemId(menuItemId);
        r.setQuantity(quantity);
        return r;
    }

    private Cart cartWith(Restaurant restaurant, MenuItem menuItem, int quantity) {
        Cart cart = new Cart();
        cart.setRestaurant(restaurant);
        CartItem ci = new CartItem();
        ci.setCart(cart);
        ci.setMenuItem(menuItem);
        ci.setQuantity(quantity);
        cart.getItems().add(ci);
        return cart;
    }

    @Test
    void rejectsItemFromDifferentRestaurant() {
        Cart cart = cartWith(restaurantA, item(10L, restaurantA), 1);
        when(cartRepository.findByCustomerIdWithItems(CUSTOMER_ID)).thenReturn(Optional.of(cart));
        when(menuItemRepository.findById(20L)).thenReturn(Optional.of(item(20L, restaurantB)));

        assertThatThrownBy(() -> service.addItem(CUSTOMER_ID, request(20L, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already has items from A");
        verify(cartRepository, never()).save(any());
    }

    @Test
    void blocksItemWithKnownAllergenConflict() {
        Cart cart = new Cart();
        when(cartRepository.findByCustomerIdWithItems(CUSTOMER_ID)).thenReturn(Optional.of(cart));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item(10L, restaurantA)));
        WarningResponse conflict = new WarningResponse(10L, "Dish 10", WarningType.KNOWN_CONFLICT, "Peanut",
                AllergySeverity.SEVERE, "Contains Peanut", true);
        when(detectionService.check(CUSTOMER_ID, List.of(10L))).thenReturn(new AllergenCheckResult(List.of(conflict)));

        assertThatThrownBy(() -> service.addItem(CUSTOMER_ID, request(10L, 1)))
                .isInstanceOf(AllergenConflictException.class);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addsSafeItemAndBindsCartToRestaurant() {
        Cart cart = new Cart();
        when(cartRepository.findByCustomerIdWithItems(CUSTOMER_ID)).thenReturn(Optional.of(cart));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item(10L, restaurantA)));
        when(detectionService.check(CUSTOMER_ID, List.of(10L))).thenReturn(AllergenCheckResult.empty());

        service.addItem(CUSTOMER_ID, request(10L, 2));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.getRestaurant()).isSameAs(restaurantA);
        verify(cartRepository).save(cart);
    }

    @Test
    void mergesQuantityForSameItemAndEnforcesLimit() {
        MenuItem dish = item(10L, restaurantA);
        Cart cart = cartWith(restaurantA, dish, 19);
        when(cartRepository.findByCustomerIdWithItems(CUSTOMER_ID)).thenReturn(Optional.of(cart));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(dish));
        when(detectionService.check(CUSTOMER_ID, List.of(10L))).thenReturn(AllergenCheckResult.empty());

        assertThatThrownBy(() -> service.addItem(CUSTOMER_ID, request(10L, 5)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at most 20");
    }

    @Test
    void rejectsUnavailableItem() {
        Cart cart = new Cart();
        MenuItem dish = item(10L, restaurantA);
        dish.setAvailable(false);
        when(cartRepository.findByCustomerIdWithItems(CUSTOMER_ID)).thenReturn(Optional.of(cart));
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(dish));

        assertThatThrownBy(() -> service.addItem(CUSTOMER_ID, request(10L, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not available");
    }
}
