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
@Table(name = "dietary_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "diet_type"}))
@Getter @Setter @NoArgsConstructor
public class DietaryPreference extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private CustomerFoodProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_type", nullable = false, length = 30)
    private DietType dietType;

    /** Strict restrictions (religious/medical) are treated as hard mismatches. */
    @Column(nullable = false)
    private boolean strict = true;

    public DietaryPreference(CustomerFoodProfile profile, DietType dietType, boolean strict) {
        this.profile = profile;
        this.dietType = dietType;
        this.strict = strict;
    }
}
