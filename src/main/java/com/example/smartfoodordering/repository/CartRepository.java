package com.example.smartfoodordering.repository;

import com.example.smartfoodordering.entity.*;
import com.example.smartfoodordering.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("""
           select c from Cart c left join fetch c.items i left join fetch i.menuItem
           where c.customer.id = :userId
           """)
    Optional<Cart> findByCustomerIdWithItems(@Param("userId") Long userId);
}
