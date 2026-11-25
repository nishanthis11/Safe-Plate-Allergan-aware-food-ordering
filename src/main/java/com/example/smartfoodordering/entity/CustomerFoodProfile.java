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
@Table(name = "customer_food_profiles")
@Getter @Setter @NoArgsConstructor
public class CustomerFoodProfile extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SpiceLevel maxSpiceLevel = SpiceLevel.MEDIUM;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "profile_favorite_cuisines", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "cuisine", length = 60)
    private Set<String> favoriteCuisines = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "profile_avoided_ingredients",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "ingredient_id"))
    private Set<Ingredient> avoidedIngredients = new HashSet<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CustomerAllergy> allergies = new HashSet<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DietaryPreference> dietaryPreferences = new HashSet<>();

    /** Customer states that their profile is complete and up to date. */
    @Column(nullable = false)
    private boolean confirmedComplete = false;
}
