package com.example.smartfoodordering.mapper;

import com.example.smartfoodordering.dto.response.MenuItemResponse;
import com.example.smartfoodordering.dto.response.RestaurantResponse;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.entity.Restaurant;
import com.example.smartfoodordering.enums.AllergenSource;
import java.util.List;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/** Must be called inside a transaction: it reads lazy collections. */
@Component
public class MenuMapper {

    public MenuItemResponse toResponse(MenuItem m) {
        List<String> declared = m.getAllergens().stream()
                .filter(a -> a.getSource() == AllergenSource.DECLARED)
                .map(a -> a.getAllergen().getName()).sorted().toList();
        List<String> mayContain = m.getAllergens().stream()
                .filter(a -> a.getSource() == AllergenSource.MAY_CONTAIN)
                .map(a -> a.getAllergen().getName()).sorted().toList();
        List<String> ingredients = m.getIngredients().stream()
                .map(i -> i.getIngredient().getName()).sorted().toList();
        return new MenuItemResponse(m.getId(), m.getRestaurant().getId(), m.getRestaurant().getName(), m.getName(),
                m.getDescription(), m.getPrice(), m.getCuisine(), m.getSpiceLevel(), m.isAvailable(),
                m.isAllergenInfoComplete(), declared, mayContain, ingredients, new TreeSet<>(m.getDietTags()));
    }

    public RestaurantResponse toResponse(Restaurant r) {
        return new RestaurantResponse(r.getId(), r.getName(), r.getCuisine(), r.getDescription(), r.getAddress(),
                r.getDeliveryFee(), r.isActive());
    }
}
