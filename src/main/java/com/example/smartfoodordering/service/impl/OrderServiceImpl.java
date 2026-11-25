package com.example.smartfoodordering.service.impl;

import com.example.smartfoodordering.dto.request.CheckoutRequest;
import com.example.smartfoodordering.dto.request.PaymentRequest;
import com.example.smartfoodordering.dto.response.AllergenCheckResult;
import com.example.smartfoodordering.dto.response.OrderDetailResponse;
import com.example.smartfoodordering.dto.response.OrderSummaryResponse;
import com.example.smartfoodordering.dto.response.PaymentResult;
import com.example.smartfoodordering.dto.response.WarningResponse;
import com.example.smartfoodordering.entity.Cart;
import com.example.smartfoodordering.entity.DeliveryAddress;
import com.example.smartfoodordering.entity.FoodOrder;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.entity.OrderAllergenWarning;
import com.example.smartfoodordering.entity.OrderItem;
import com.example.smartfoodordering.entity.OrderStatusHistory;
import com.example.smartfoodordering.entity.Payment;
import com.example.smartfoodordering.entity.Restaurant;
import com.example.smartfoodordering.entity.User;
import com.example.smartfoodordering.enums.AuditAction;
import com.example.smartfoodordering.enums.OrderStatus;
import com.example.smartfoodordering.enums.PaymentStatus;
import com.example.smartfoodordering.exception.AccessDeniedAppException;
import com.example.smartfoodordering.exception.AllergenAcknowledgementRequiredException;
import com.example.smartfoodordering.exception.AllergenConflictException;
import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.exception.PaymentFailedException;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.mapper.OrderMapper;
import com.example.smartfoodordering.repository.CartRepository;
import com.example.smartfoodordering.repository.DeliveryAddressRepository;
import com.example.smartfoodordering.repository.FoodOrderRepository;
import com.example.smartfoodordering.repository.MenuItemRepository;
import com.example.smartfoodordering.repository.UserRepository;
import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.AllergenDetectionService;
import com.example.smartfoodordering.service.AuditService;
import com.example.smartfoodordering.service.FoodProfileService;
import com.example.smartfoodordering.service.OrderService;
import com.example.smartfoodordering.service.PaymentGateway;
import com.example.smartfoodordering.util.OrderNumberGenerator;
import com.example.smartfoodordering.util.PricingUtil;
import com.example.smartfoodordering.util.TextUtil;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    private static final Set<OrderStatus> KITCHEN_STATUSES = EnumSet.of(OrderStatus.PREPARING, OrderStatus.READY);

    private final FoodOrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final DeliveryAddressRepository addressRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final AllergenDetectionService detectionService;
    private final FoodProfileService foodProfileService;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;
    private final OrderMapper orderMapper;

    public OrderServiceImpl(FoodOrderRepository orderRepository, CartRepository cartRepository,
                            DeliveryAddressRepository addressRepository, MenuItemRepository menuItemRepository,
                            UserRepository userRepository, AllergenDetectionService detectionService,
                            FoodProfileService foodProfileService, PaymentGateway paymentGateway,
                            AuditService auditService, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
        this.detectionService = detectionService;
        this.foodProfileService = foodProfileService;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
        this.orderMapper = orderMapper;
    }

    // ---------------------------------------------------------------- customer

    @Override
    public OrderDetailResponse placeOrder(Long customerId, String customerEmail, CheckoutRequest request) {
        Cart cart = cartRepository.findByCustomerIdWithItems(customerId)
                .orElseThrow(() -> new BusinessRuleException("Your cart is empty"));
        if (cart.getItems().isEmpty() || cart.getRestaurant() == null) {
            throw new BusinessRuleException("Your cart is empty");
        }
        Restaurant restaurant = cart.getRestaurant();
        if (!restaurant.isActive()) {
            throw new BusinessRuleException(restaurant.getName() + " is not accepting orders right now");
        }
        DeliveryAddress address = addressRepository.findByIdAndUserId(request.getAddressId(), customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery address not found"));

        // Re-validate everything on the server: availability, restaurant, prices, allergens.
        List<Long> itemIds = cart.getItems().stream().map(ci -> ci.getMenuItem().getId()).toList();
        Map<Long, MenuItem> items = menuItemRepository.findAllWithSafetyDataByIdIn(itemIds).stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));
        for (Long id : itemIds) {
            MenuItem m = items.get(id);
            if (m == null || !m.isAvailable() || !m.getRestaurant().getId().equals(restaurant.getId())) {
                throw new BusinessRuleException("An item in your cart is no longer available. Please review your cart.");
            }
        }

        AllergenCheckResult check = detectionService.check(customerId, itemIds);
        if (check.hasBlocking()) {
            auditService.log(customerEmail, AuditAction.ORDER_PLACED, "Cart", cart.getId(),
                    "Blocked by allergen conflict: " + check.summary());
            throw new AllergenConflictException(check);
        }
        if (check.requiresAcknowledgement() && !request.isAcknowledgeWarnings()) {
            throw new AllergenAcknowledgementRequiredException(check);
        }

        FoodOrder order = new FoodOrder();
        order.setOrderNumber(OrderNumberGenerator.next());
        order.setCustomer(userRepository.getReferenceById(customerId));
        order.setRestaurant(restaurant);
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddressSnapshot(TextUtil.truncate(address.getLabel() + ": " + address.asSingleLine(), 500));
        order.setDeliveryPhoneSnapshot(address.getContactPhone());
        order.setProfileSnapshot(foodProfileService.buildSnapshot(customerId));
        order.setWarningsAcknowledged(check.hasWarnings());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (var cartItem : cart.getItems()) {
            MenuItem m = items.get(cartItem.getMenuItem().getId());
            OrderItem oi = new OrderItem();
            oi.setMenuItem(m);
            oi.setItemNameSnapshot(m.getName());
            oi.setUnitPriceSnapshot(m.getPrice());
            oi.setQuantity(cartItem.getQuantity());
            oi.setLineTotal(PricingUtil.lineTotal(m.getPrice(), cartItem.getQuantity()));
            oi.setSpecialInstructions(cartItem.getSpecialInstructions());
            oi.setAllergensSnapshot(TextUtil.truncate(m.getAllergens().stream()
                    .map(a -> a.getAllergen().getName() + " (" + a.getSource() + ")").sorted()
                    .collect(Collectors.joining(", ")), 600));
            oi.setIngredientsSnapshot(TextUtil.truncate(m.getIngredients().stream()
                    .map(i -> i.getIngredient().getName()).sorted().collect(Collectors.joining(", ")), 1000));
            order.addItem(oi);
            subtotal = subtotal.add(oi.getLineTotal());
        }
        subtotal = subtotal.setScale(2);
        BigDecimal tax = PricingUtil.tax(subtotal);
        BigDecimal fee = restaurant.getDeliveryFee();
        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setDeliveryFee(fee);
        order.setTotal(PricingUtil.total(subtotal, tax, fee));

        for (WarningResponse w : check.warnings()) {
            OrderAllergenWarning snapshot = new OrderAllergenWarning();
            snapshot.setItemName(w.itemName());
            snapshot.setWarningType(w.type());
            snapshot.setSubject(w.subject());
            snapshot.setSeverity(w.severity());
            snapshot.setMessage(TextUtil.truncate(w.message(), 400));
            order.addWarning(snapshot);
        }

        PaymentResult result = paymentGateway.charge(new PaymentRequest(order.getOrderNumber(), order.getTotal(),
                request.getPaymentMethod(), request.isSimulatePaymentFailure()));
        auditService.log(customerEmail, AuditAction.PAYMENT_PROCESSED, "Order", null,
                order.getOrderNumber() + " " + request.getPaymentMethod() + " -> " + result.status());
        if (!result.success()) {
            throw new PaymentFailedException(result.message() + ". Your cart is unchanged; please try again.");
        }
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotal());
        payment.setMethod(request.getPaymentMethod());
        payment.setStatus(result.status());
        payment.setTransactionReference(result.reference());
        payment.setProvider(paymentGateway.providerName());
        order.setPayment(payment);

        order.addHistory(new OrderStatusHistory(null, OrderStatus.PENDING, customerEmail, "Order placed"));
        FoodOrder saved = orderRepository.save(order);

        cart.clear();
        cartRepository.save(cart);

        auditService.log(customerEmail, AuditAction.ORDER_PLACED, "Order", saved.getId(), saved.getOrderNumber());
        if (check.hasWarnings()) {
            auditService.log(customerEmail, AuditAction.ALLERGEN_OVERRIDE_ACKNOWLEDGED, "Order", saved.getId(),
                    check.warnings().size() + " warning(s) acknowledged");
        }
        return orderMapper.toDetail(saved, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listForCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(orderMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getForCustomer(Long customerId, Long orderId) {
        FoodOrder order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return orderMapper.toDetail(order, List.of());
    }

    @Override
    public void cancelByCustomer(Long customerId, String customerEmail, Long orderId, String reason) {
        FoodOrder order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BusinessRuleException("This order can no longer be cancelled");
        }
        applyStatus(order, OrderStatus.CANCELLED, customerEmail, reason == null || reason.isBlank()
                ? "Cancelled by customer" : reason.trim());
        auditService.log(customerEmail, AuditAction.ORDER_CANCELLED, "Order", order.getId(), order.getOrderNumber());
    }

    // ------------------------------------------------------------------- staff

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listForStaff(CustomUserDetails actor) {
        List<FoodOrder> orders = actor.isAdmin() ? orderRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList()
                : orderRepository.findByRestaurantIdOrderByCreatedAtDesc(requireRestaurant(actor));
        return orders.stream().map(orderMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> kitchenQueue(CustomUserDetails actor) {
        return orderRepository.findByRestaurantIdAndStatusInOrderByCreatedAtAsc(requireRestaurant(actor),
                        EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING)).stream()
                .map(orderMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getForStaff(CustomUserDetails actor, Long orderId) {
        FoodOrder order = loadForStaff(actor, orderId);
        return orderMapper.toDetail(order, allowedNext(actor, order.getStatus()));
    }

    @Override
    public OrderDetailResponse updateStatus(CustomUserDetails actor, Long orderId, OrderStatus next, String note) {
        FoodOrder order = loadForStaff(actor, orderId);
        if (!allowedNext(actor, order.getStatus()).contains(next)) {
            throw new BusinessRuleException("Cannot move an order from " + order.getStatus() + " to " + next);
        }
        applyStatus(order, next, actor.getUsername(), note);
        auditService.log(actor.getUsername(), AuditAction.ORDER_STATUS_CHANGED, "Order", order.getId(),
                order.getOrderNumber() + " -> " + next);
        return orderMapper.toDetail(order, allowedNext(actor, order.getStatus()));
    }

    // --------------------------------------------------------------- scheduler

    @Override
    public int cancelStalePendingOrders(Duration olderThan) {
        List<FoodOrder> stale = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING,
                LocalDateTime.now().minus(olderThan));
        for (FoodOrder order : stale) {
            applyStatus(order, OrderStatus.CANCELLED, "system", "Auto-cancelled: restaurant did not confirm in time");
            auditService.log("system", AuditAction.ORDER_CANCELLED, "Order", order.getId(), order.getOrderNumber());
        }
        return stale.size();
    }

    // ----------------------------------------------------------------- helpers

    private void applyStatus(FoodOrder order, OrderStatus next, String by, String note) {
        OrderStatus previous = order.getStatus();
        if (!previous.canTransitionTo(next)) {
            throw new BusinessRuleException("Invalid status change " + previous + " -> " + next);
        }
        order.setStatus(next);
        if (next == OrderStatus.CANCELLED) {
            order.setCancellationReason(TextUtil.truncate(note, 250));
            Payment payment = order.getPayment();
            if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
                PaymentResult refund = paymentGateway.refund(payment.getTransactionReference(), payment.getAmount());
                payment.setStatus(refund.status());
            }
        }
        order.addHistory(new OrderStatusHistory(previous, next, by, TextUtil.truncate(note, 250)));
        orderRepository.save(order);
    }

    /** Status values this actor may move the order to next, according to role and the lifecycle rules. */
    private List<OrderStatus> allowedNext(CustomUserDetails actor, OrderStatus current) {
        return current.allowedNext().stream()
                .filter(s -> actor.isAdmin() || actor.isManager() || (actor.isKitchen() && KITCHEN_STATUSES.contains(s)))
                .sorted().toList();
    }

    private FoodOrder loadForStaff(CustomUserDetails actor, Long orderId) {
        FoodOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!actor.isAdmin()) {
            Long restaurantId = actor.getRestaurantId();
            if (restaurantId == null || !restaurantId.equals(order.getRestaurant().getId())) {
                auditService.log(actor.getUsername(), AuditAction.ACCESS_DENIED, "Order", orderId,
                        "Attempted access to another restaurant's order");
                throw new AccessDeniedAppException("You cannot access this order");
            }
        }
        return order;
    }

    private Long requireRestaurant(CustomUserDetails actor) {
        if (actor.getRestaurantId() == null) {
            throw new BusinessRuleException("Your account is not linked to a restaurant");
        }
        return actor.getRestaurantId();
    }
}
