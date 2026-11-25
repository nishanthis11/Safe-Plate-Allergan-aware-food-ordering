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
@Table(name = "order_allergen_warnings")
@Getter @Setter @NoArgsConstructor
public class OrderAllergenWarning extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private FoodOrder order;

    @Column(nullable = false, length = 120)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WarningType warningType;

    /** Allergen code or diet name that triggered the warning (nullable for INFORMATION_INCOMPLETE). */
    @Column(length = 60)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private AllergySeverity severity;

    @Column(nullable = false, length = 400)
    private String message;
}
