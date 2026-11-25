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
@Table(name = "menu_item_ingredients",
        uniqueConstraints = @UniqueConstraint(columnNames = {"menu_item_id", "ingredient_id"}))
@Getter @Setter @NoArgsConstructor
public class MenuItemIngredient extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    public MenuItemIngredient(MenuItem menuItem, Ingredient ingredient) {
        this.menuItem = menuItem;
        this.ingredient = ingredient;
    }
}
