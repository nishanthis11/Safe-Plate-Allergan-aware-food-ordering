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
@Table(name = "menu_item_allergens",
        uniqueConstraints = @UniqueConstraint(columnNames = {"menu_item_id", "allergen_id"}))
@Getter @Setter @NoArgsConstructor
public class MenuItemAllergen extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allergen_id", nullable = false)
    private Allergen allergen;

    /** DECLARED = contains; MAY_CONTAIN = cross-contamination risk. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AllergenSource source = AllergenSource.DECLARED;

    public MenuItemAllergen(MenuItem menuItem, Allergen allergen, AllergenSource source) {
        this.menuItem = menuItem;
        this.allergen = allergen;
        this.source = source;
    }
}
