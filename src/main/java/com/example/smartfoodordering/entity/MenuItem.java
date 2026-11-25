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
@Table(name = "menu_items", indexes = @Index(name = "idx_menu_restaurant", columnList = "restaurant_id"))
@Getter @Setter @NoArgsConstructor
public class MenuItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 600)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 60)
    private String cuisine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SpiceLevel spiceLevel = SpiceLevel.NONE;

    @Column(nullable = false)
    private boolean available = true;

    /** Manager confirms that ingredient and allergen declarations are complete. If false -> INFORMATION_INCOMPLETE. */
    @Column(nullable = false)
    private boolean allergenInfoComplete = false;

    /** Diets this item is explicitly declared compliant with. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "menu_item_diet_tags", joinColumns = @JoinColumn(name = "menu_item_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "diet_type", length = 30)
    private Set<DietType> dietTags = new HashSet<>();

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MenuItemIngredient> ingredients = new HashSet<>();

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MenuItemAllergen> allergens = new HashSet<>();
}
