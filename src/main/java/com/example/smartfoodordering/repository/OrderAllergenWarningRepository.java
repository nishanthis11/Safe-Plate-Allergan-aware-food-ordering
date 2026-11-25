package com.example.smartfoodordering.repository;

import com.example.smartfoodordering.entity.*;
import com.example.smartfoodordering.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface OrderAllergenWarningRepository extends JpaRepository<OrderAllergenWarning, Long> {
    @Query("select w.warningType, count(w) from OrderAllergenWarning w "
         + "where (:restaurantId is null or w.order.restaurant.id = :restaurantId) group by w.warningType")
    List<Object[]> countByType(@Param("restaurantId") Long restaurantId);
}
