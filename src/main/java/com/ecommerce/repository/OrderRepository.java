package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Order operations in ecommerce_db.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerIdOrderByOrderDateDesc(Long buyerId);

    List<Order> findByBuyerEmailOrderByOrderDateDesc(String email);

    Optional<Order> findByIdAndBuyerId(Long id, Long buyerId);

    @Query("SELECT o FROM Order o JOIN FETCH o.buyer ORDER BY o.orderDate DESC")
    List<Order> findAllWithBuyerOrderByOrderDateDesc();

    List<Order> findAllByOrderByOrderDateDesc();
}
