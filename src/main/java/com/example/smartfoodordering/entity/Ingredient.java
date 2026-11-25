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
@Table(name = "ingredients")
@Getter @Setter @NoArgsConstructor
public class Ingredient extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngredientCategory category = IngredientCategory.OTHER;

    /** Allergens this ingredient commonly contains or derives from (drives POSSIBLE_CONFLICT detection). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "ingredient_common_allergens",
            joinColumns = @JoinColumn(name = "ingredient_id"),
            inverseJoinColumns = @JoinColumn(name = "allergen_id"))
    private Set<Allergen> commonAllergens = new HashSet<>();

    /** Diets this ingredient is incompatible with (drives DIETARY_MISMATCH detection). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ingredient_incompatible_diets", joinColumns = @JoinColumn(name = "ingredient_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "diet_type", length = 30)
    private Set<DietType> incompatibleDiets = new HashSet<>();

    public Ingredient(String name, IngredientCategory category) {
        this.name = name;
        this.category = category;
    }
}
