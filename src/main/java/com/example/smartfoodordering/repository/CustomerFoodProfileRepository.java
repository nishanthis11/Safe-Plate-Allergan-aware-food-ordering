package com.example.smartfoodordering.repository;

import com.example.smartfoodordering.entity.*;
import com.example.smartfoodordering.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface CustomerFoodProfileRepository extends JpaRepository<CustomerFoodProfile, Long> {
    Optional<CustomerFoodProfile> findByUserId(Long userId);

    @Query("""
           select distinct p from CustomerFoodProfile p
           left join fetch p.allergies a left join fetch a.allergen
           left join fetch p.dietaryPreferences
           where p.user.id = :userId
           """)
    Optional<CustomerFoodProfile> findDetailedByUserId(@Param("userId") Long userId);
}
