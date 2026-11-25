package com.example.smartfoodordering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.smartfoodordering.dto.request.CheckoutRequest;
import com.example.smartfoodordering.dto.request.PaymentRequest;
import com.example.smartfoodordering.dto.response.AllergenCheckResult;
import com.example.smartfoodordering.dto.response.OrderDetailResponse;
import com.example.smartfoodordering.dto.response.PaymentResult;
import com.example.smartfoodordering.dto.response.WarningResponse;
import com.example.smartfoodordering.entity.Cart;
import com.example.smartfoodordering.entity.CartItem;
import com.example.smartfoodordering.entity.DeliveryAddress;
import com.example.smartfoodordering.entity.FoodOrder;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.entity.Restaurant;
import com.example.smartfoodordering.entity.Role;
import com.example.smartfoodordering.entity.User;
import com.example.smartfoodordering.enums.AllergySeverity;
import com.example.smartfoodordering.enums.AuditAction;
import com.example.smartfoodordering.enums.OrderStatus;
import com.example.smartfoodordering.enums.PaymentStatus;
import com.example.smartfoodordering.enums.RoleName;
import com.example.smartfoodordering.enums.WarningType;
import com.example.smartfoodordering.exception.AccessDeniedAppException;
import com.example.smartfoodordering.exception.AllergenAcknowledgementRequiredException;
import com.example.smartfoodordering.exception.AllergenConflictException;
import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.exception.PaymentFailedException;
import com.example.smartfoodordering.mapper.OrderMapper;
import com.example.smartfoodordering.repository.CartRepository;
import com.example.smartfoodordering.repository.DeliveryAddressRepository;
import com.example.smartfoodordering.repository.FoodOrderRepository;
import com.example.smartfoodordering.repository.MenuItemRepository;
import com.example.smartfoodordering.repository.UserRepository;
import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.impl.OrderServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    private static final Long CUSTOMER_ID = 1L;
    private static final String EMAIL = "priya@example.com";

    @Mock
    private FoodOrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private DeliveryAddressRepository addressRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AllergenDetectionService detectionService;
    @Mock
    private FoodProfileService foodProfileService;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private AuditService auditService;

    private OrderServiceImpl service;
    private Restaurant restaurant;
    private MenuItem dish;
    private Cart cart;
    private CheckoutRequest request;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, cartRepository, addressRepository, menuItemRepository,
                userRepository, detectionService, foodProfileService, paymentGateway, auditService, new OrderMapper());

        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setName("Green Leaf");

        dish = new MenuItem();
        dish.setId(10L);
        dish.setName("Dish");
        dish.setPrice(new BigDecimal("200.00"));
        dish.setRestaurant(restaurant);

        cart = new Cart();
        cart.setRestaurant(restaurant);
        CartItem ci = new CartItem();
        ci.setCart(cart);
        ci.setMenuItem(dish);
        ci.setQuantity(2);
        cart.getItems().add(ci);

        request = new CheckoutRequest();
        request.setAddressId(5L);
    }

    /** Stubs the lookups every checkout performs before the safety check. */
    private void stubCheckoutLookups() {
        DeliveryAddress address = new DeliveryAddress();
        address.setLabel("Home");
        address.setLine1("22 Lake View Street");
        address.setCity("Tiruchengode");
        address.setState("Tamil Nadu");
        address.setPostalCode("637211");
        address.setContactPhone("9876543210");
        when(cartRepository.findByCustomerIdWithItems(CUSTOMER_ID)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(5L, CUSTOMER_ID)).thenReturn(Optional.of(address));
        when(menuItemRepository.findAllWithSafetyDataByIdIn(List.of(10L))).thenReturn(List.of(dish));
    }

    private WarningResponse warning(WarningType type, boolean blocking) {
        return new WarningResponse(10L, "Dish", type, "Peanut", AllergySeverity.SEVERE, "msg", blocking);
    }

    @Test
    void knownAllergenConflictBlocksCheckoutEvenIfAcknowledged() {
        stubCheckoutLookups();
        request.setAcknowledgeWarnings(true);
        when(detectionService.check(CUSTOMER_ID, List.of(10L)))
                .thenReturn(new AllergenCheckResult(List.of(warning(WarningType.KNOWN_CONFLICT, true))));

        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, EMAIL, request))
                .isInstanceOf(AllergenConflictException.class);
        verify(orderRepository, never()).save(any());
        verify(auditService).log(eq(EMAIL), eq(AuditAction.ORDER_PLACED), eq("Cart"), any(), anyString());
    }

    @Test
    void nonBlockingWarningsRequireAcknowledgement() {
        stubCheckoutLookups();
        request.setAcknowledgeWarnings(false);
        when(detectionService.check(CUSTOMER_ID, List.of(10L)))
                .thenReturn(new AllergenCheckResult(List.of(warning(WarningType.POSSIBLE_CONFLICT, false))));

        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, EMAIL, request))
                .isInstanceOf(AllergenAcknowledgementRequiredException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void failedPaymentDoesNotCreateOrder() {
        stubCheckoutLookups();
        User user = new User();
        user.setFullName("Priya");
        when(detectionService.check(CUSTOMER_ID, List.of(10L))).thenReturn(AllergenCheckResult.empty());
        when(userRepository.getReferenceById(CUSTOMER_ID)).thenReturn(user);
        when(foodProfileService.buildSnapshot(CUSTOMER_ID)).thenReturn("snapshot");
        when(paymentGateway.charge(any(PaymentRequest.class)))
                .thenReturn(new PaymentResult(PaymentStatus.FAILED, null, "Declined"));

        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, EMAIL, request))
                .isInstanceOf(PaymentFailedException.class);
        verify(orderRepository, never()).save(any());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void successfulCheckoutRecalculatesPricesOnServerAndClearsCart() {
        stubCheckoutLookups();
        User user = new User();
        user.setFullName("Priya");
        when(detectionService.check(CUSTOMER_ID, List.of(10L))).thenReturn(AllergenCheckResult.empty());
        when(userRepository.getReferenceById(CUSTOMER_ID)).thenReturn(user);
        when(foodProfileService.buildSnapshot(CUSTOMER_ID)).thenReturn("snapshot");
        when(paymentGateway.charge(any(PaymentRequest.class)))
                .thenReturn(new PaymentResult(PaymentStatus.SUCCESS, "SBX-1", "ok"));
        when(paymentGateway.providerName()).thenReturn("sandbox");
        when(orderRepository.save(any(FoodOrder.class))).thenAnswer(invocation -> {
            FoodOrder saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        OrderDetailResponse result = service.placeOrder(CUSTOMER_ID, EMAIL, request);

        // 2 x 200.00 = 400.00, tax 5% = 20.00, delivery 30.00
        assertThat(result.subtotal()).isEqualByComparingTo("400.00");
        assertThat(result.tax()).isEqualByComparingTo("20.00");
        assertThat(result.total()).isEqualByComparingTo("450.00");
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.history()).hasSize(1);
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getRestaurant()).isNull();
        verify(auditService).log(eq(EMAIL), eq(AuditAction.ORDER_PLACED), eq("Order"), eq(99L), anyString());
    }

    // ------------------------------------------------------------ staff access

    private CustomUserDetails staff(RoleName role, Long restaurantId) {
        User user = new User();
        user.setEmail("staff@example.com");
        user.setPasswordHash("x");
        user.setFullName("Staff");
        user.getRoles().add(new Role(role));
        if (restaurantId != null) {
            Restaurant r = new Restaurant();
            r.setId(restaurantId);
            user.setRestaurant(r);
        }
        user.setId(50L);
        return new CustomUserDetails(user);
    }

    private FoodOrder pendingOrder() {
        User customer = new User();
        customer.setFullName("Priya");
        FoodOrder order = new FoodOrder();
        order.setId(77L);
        order.setOrderNumber("SF-TEST");
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setStatus(OrderStatus.PENDING);
        return order;
    }

    @Test
    void managerOfSameRestaurantCanConfirmPendingOrder() {
        when(orderRepository.findById(77L)).thenReturn(Optional.of(pendingOrder()));

        OrderDetailResponse result = service.updateStatus(staff(RoleName.ROLE_RESTAURANT_MANAGER, 1L), 77L,
                OrderStatus.CONFIRMED, "ok");

        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.history()).hasSize(1);
    }

    @Test
    void staffOfAnotherRestaurantIsDenied() {
        when(orderRepository.findById(77L)).thenReturn(Optional.of(pendingOrder()));

        assertThatThrownBy(() -> service.updateStatus(staff(RoleName.ROLE_RESTAURANT_MANAGER, 2L), 77L,
                OrderStatus.CONFIRMED, null)).isInstanceOf(AccessDeniedAppException.class);
        verify(auditService).log(eq("staff@example.com"), eq(AuditAction.ACCESS_DENIED), eq("Order"), eq(77L), anyString());
    }

    @Test
    void kitchenStaffCannotConfirmOrCancel() {
        when(orderRepository.findById(77L)).thenReturn(Optional.of(pendingOrder()));

        assertThatThrownBy(() -> service.updateStatus(staff(RoleName.ROLE_KITCHEN_STAFF, 1L), 77L,
                OrderStatus.CONFIRMED, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void invalidLifecycleJumpIsRejected() {
        when(orderRepository.findById(77L)).thenReturn(Optional.of(pendingOrder()));

        assertThatThrownBy(() -> service.updateStatus(staff(RoleName.ROLE_RESTAURANT_MANAGER, 1L), 77L,
                OrderStatus.DELIVERED, null)).isInstanceOf(BusinessRuleException.class);
    }
}
