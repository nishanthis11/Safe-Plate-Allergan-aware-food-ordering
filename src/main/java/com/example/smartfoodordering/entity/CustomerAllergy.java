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
@Table(name = "customer_allergies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "allergen_id"}))
@Getter @Setter @NoArgsConstructor
public class CustomerAllergy extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private CustomerFoodProfile profile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allergen_id", nullable = false)
    private Allergen allergen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AllergySeverity severity = AllergySeverity.MODERATE;

    @Column(length = 300)
    private String notes;
}
