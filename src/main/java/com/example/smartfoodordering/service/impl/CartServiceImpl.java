package com.example.smartfoodordering.service.impl;

import com.example.smartfoodordering.dto.request.CartAddRequest;
import com.example.smartfoodordering.dto.response.AllergenCheckResult;
import com.example.smartfoodordering.dto.response.CartItemResponse;
import com.example.smartfoodordering.dto.response.CartResponse;
import com.example.smartfoodordering.entity.Cart;
import com.example.smartfoodordering.entity.CartItem;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.entity.User;
import com.example.smartfoodordering.exception.AllergenConflictException;
import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.repository.CartRepository;
import com.example.smartfoodordering.repository.MenuItemRepository;
import com.example.smartfoodordering.repository.UserRepository;
import com.example.smartfoodordering.service.AllergenDetectionService;
import com.example.smartfoodordering.service.CartService;
import com.example.smartfoodordering.util.PricingUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartServiceImpl implements CartService {
    static final int MAX_QUANTITY_PER_ITEM = 20;

    private final CartRepository cartRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final AllergenDetectionService detectionService;

    public CartServiceImpl(CartRepository cartRepository, MenuItemRepository menuItemRepository,
                           UserRepository userRepository, AllergenDetectionService detectionService) {
        this.cartRepository = cartRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
        this.detectionService = detectionService;
    }

    @Override
    public CartResponse getCart(Long customerId) {
        Optional<Cart> cart = cartRepository.findByCustomerIdWithItems(customerId);
        if (cart.isEmpty() || cart.get().getItems().isEmpty()) {
            BigDecimal zero = BigDecimal.ZERO.setScale(2);
            return new CartResponse(null, null, List.of(), zero, zero, zero, zero, AllergenCheckResult.empty());
        }
        return toResponse(customerId, cart.get());
    }

    @Override
    public void addItem(Long customerId, CartAddRequest request) {
        Cart cart = loadOrCreate(customerId);
        MenuItem item = menuItemRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        if (!item.isAvailable() || !item.getRestaurant().isActive()) {
            throw new BusinessRuleException("This item is not available right now");
        }
        if (cart.getRestaurant() != null && !cart.getItems().isEmpty()
                && !cart.getRestaurant().getId().equals(item.getRestaurant().getId())) {
            throw new BusinessRuleException("Your cart already has items from " + cart.getRestaurant().getName()
                    + ". Clear the cart or checkout first to order from another restaurant.");
        }
        AllergenCheckResult check = detectionService.check(customerId, List.of(item.getId()));
        if (check.hasBlocking()) {
            throw new AllergenConflictException(check);
        }

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(ci -> ci.getMenuItem().getId().equals(item.getId())).findFirst();
        if (existing.isPresent()) {
            int newQuantity = existing.get().getQuantity() + request.getQuantity();
            if (newQuantity > MAX_QUANTITY_PER_ITEM) {
                throw new BusinessRuleException("You can order at most " + MAX_QUANTITY_PER_ITEM + " of one item");
            }
            existing.get().setQuantity(newQuantity);
            if (request.getSpecialInstructions() != null && !request.getSpecialInstructions().isBlank()) {
                existing.get().setSpecialInstructions(request.getSpecialInstructions().trim());
            }
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setMenuItem(item);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setSpecialInstructions(request.getSpecialInstructions() == null
                    || request.getSpecialInstructions().isBlank() ? null : request.getSpecialInstructions().trim());
            cart.getItems().add(cartItem);
        }
        cart.setRestaurant(item.getRestaurant());
        cartRepository.save(cart);
    }

    @Override
    public void updateQuantity(Long customerId, Long cartItemId, int quantity) {
        Cart cart = loadOrCreate(customerId);
        CartItem cartItem = cart.getItems().stream().filter(ci -> ci.getId().equals(cartItemId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (quantity <= 0) {
            cart.getItems().remove(cartItem);
        } else if (quantity > MAX_QUANTITY_PER_ITEM) {
            throw new BusinessRuleException("You can order at most " + MAX_QUANTITY_PER_ITEM + " of one item");
        } else {
            cartItem.setQuantity(quantity);
        }
        if (cart.getItems().isEmpty()) {
            cart.setRestaurant(null);
        }
        cartRepository.save(cart);
    }

    @Override
    public void removeItem(Long customerId, Long cartItemId) {
        updateQuantity(customerId, cartItemId, 0);
    }

    @Override
    public void clear(Long customerId) {
        cartRepository.findByCustomerIdWithItems(customerId).ifPresent(cart -> {
            cart.clear();
            cartRepository.save(cart);
        });
    }

    private Cart loadOrCreate(Long customerId) {
        return cartRepository.findByCustomerIdWithItems(customerId).orElseGet(() -> {
            User user = userRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Cart cart = new Cart();
            cart.setCustomer(user);
            return cart;
        });
    }

    /** Prices always come from the current menu, never from anything the browser sent. */
    private CartResponse toResponse(Long customerId, Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(ci -> {
            BigDecimal unit = ci.getMenuItem().getPrice();
            return new CartItemResponse(ci.getId(), ci.getMenuItem().getId(), ci.getMenuItem().getName(), unit,
                    ci.getQuantity(), PricingUtil.lineTotal(unit, ci.getQuantity()), ci.getSpecialInstructions());
        }).toList();
        BigDecimal subtotal = items.stream().map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        BigDecimal tax = PricingUtil.tax(subtotal);
        BigDecimal fee = cart.getRestaurant().getDeliveryFee();
        List<Long> ids = cart.getItems().stream().map(ci -> ci.getMenuItem().getId()).toList();
        AllergenCheckResult check = detectionService.check(customerId, ids);
        return new CartResponse(cart.getRestaurant().getId(), cart.getRestaurant().getName(), items, subtotal, tax,
                fee, PricingUtil.total(subtotal, tax, fee), check);
    }
}
