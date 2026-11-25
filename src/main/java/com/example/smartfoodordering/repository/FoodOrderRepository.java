package com.example.smartfoodordering.repository;

import com.example.smartfoodordering.entity.*;
import com.example.smartfoodordering.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
    List<FoodOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<FoodOrder> findByIdAndCustomerId(Long id, Long customerId);
    Optional<FoodOrder> findByIdAndRestaurantId(Long id, Long restaurantId);
    List<FoodOrder> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    List<FoodOrder> findByRestaurantIdAndStatusInOrderByCreatedAtAsc(Long restaurantId, Collection<OrderStatus> statuses);
    List<FoodOrder> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime before);

    @Query("select o.status, count(o) from FoodOrder o where (:restaurantId is null or o.restaurant.id = :restaurantId) group by o.status")
    List<Object[]> countByStatus(@Param("restaurantId") Long restaurantId);

    @Query("""
           select sum(o.total) from FoodOrder o
           where o.status <> com.example.smartfoodordering.enums.OrderStatus.CANCELLED
             and (:restaurantId is null or o.restaurant.id = :restaurantId)
           """)
    BigDecimal totalRevenue(@Param("restaurantId") Long restaurantId);

    List<FoodOrder> findByCreatedAtAfterAndStatusNot(LocalDateTime since, OrderStatus status);

    List<FoodOrder> findByRestaurantIdAndCreatedAtAfterAndStatusNot(Long restaurantId, LocalDateTime since, OrderStatus status);

    @Query("""
           select oi.itemNameSnapshot, sum(oi.quantity) from OrderItem oi
           where oi.order.status <> com.example.smartfoodordering.enums.OrderStatus.CANCELLED
             and (:restaurantId is null or oi.order.restaurant.id = :restaurantId)
           group by oi.itemNameSnapshot order by sum(oi.quantity) desc
           """)
    List<Object[]> topItems(@Param("restaurantId") Long restaurantId, org.springframework.data.domain.Pageable pageable);
}
