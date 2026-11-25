package com.example.smartfoodordering.repository;

import com.example.smartfoodordering.entity.*;
import com.example.smartfoodordering.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantIdOrderByName(Long restaurantId);
    List<MenuItem> findByRestaurantIdAndAvailableTrueOrderByName(Long restaurantId);

    @Query("select distinct m from MenuItem m join fetch m.restaurant r where m.available = true and r.active = true")
    List<MenuItem> findAllOrderable();

    @Query("""
           select distinct m from MenuItem m
           left join fetch m.ingredients mi left join fetch mi.ingredient
           left join fetch m.allergens ma left join fetch ma.allergen
           where m.id in :ids
           """)
    List<MenuItem> findAllWithSafetyDataByIdIn(@Param("ids") Collection<Long> ids);

    @Query("""
           select distinct m from MenuItem m
           left join fetch m.ingredients mi left join fetch mi.ingredient i left join fetch i.commonAllergens
           left join fetch m.allergens ma left join fetch ma.allergen
           where m.available = true and m.restaurant.active = true
           """)
    List<MenuItem> findAllOrderableWithSafetyData();
}
