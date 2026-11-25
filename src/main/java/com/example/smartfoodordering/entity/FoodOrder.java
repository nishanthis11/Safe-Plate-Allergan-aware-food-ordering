package com.example.smartfoodordering.entity;

import com.example.smartfoodordering.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "food_orders", indexes = {
        @Index(name = "idx_order_customer", columnList = "customer_id"),
        @Index(name = "idx_order_restaurant_status", columnList = "restaurant_id,status")})
@Getter @Setter @NoArgsConstructor
public class FoodOrder extends BaseEntity {
    @Column(nullable = false, unique = true, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    /** Snapshot of delivery address at order time (address book entries may change later). */
    @Column(nullable = false, length = 500)
    private String deliveryAddressSnapshot;

    @Column(length = 20)
    private String deliveryPhoneSnapshot;

    /** Snapshot of the customer's allergies / diets / avoided ingredients at order time. */
    @Column(length = 2000)
    private String profileSnapshot;

    /** Customer acknowledged non-blocking warnings (POSSIBLE_CONFLICT / INFORMATION_INCOMPLETE / DIETARY_MISMATCH). */
    @Column(nullable = false)
    private boolean warningsAcknowledged = false;

    @Column(length = 250)
    private String cancellationReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderAllergenWarning> warnings = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<OrderStatusHistory> history = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    public void addItem(OrderItem item) { item.setOrder(this); items.add(item); }
    public void addWarning(OrderAllergenWarning w) { w.setOrder(this); warnings.add(w); }
    public void addHistory(OrderStatusHistory h) { h.setOrder(this); history.add(h); }
}
