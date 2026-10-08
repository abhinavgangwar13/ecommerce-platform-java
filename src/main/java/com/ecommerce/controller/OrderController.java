package com.ecommerce.controller;

import com.ecommerce.dto.OrderResponse;
import com.ecommerce.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing RESTful Order Management endpoints strictly for BUYER users.
 */
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('BUYER')")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Place a new order from current buyer's cart.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(Authentication authentication) {
        String email = authentication.getName();
        OrderResponse response = orderService.placeOrder(email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * View order history for the authenticated buyer.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrderHistory(Authentication authentication) {
        String email = authentication.getName();
        List<OrderResponse> response = orderService.getBuyerOrders(email);
        return ResponseEntity.ok(response);
    }

    /**
     * View details of a specific order belonging to the buyer.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable Long id,
                                                         Authentication authentication) {
        String email = authentication.getName();
        OrderResponse response = orderService.getOrderById(id, email);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel an order in PLACED status and restore inventory stock.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id,
                                                     Authentication authentication) {
        String email = authentication.getName();
        OrderResponse response = orderService.cancelOrder(id, email);
        return ResponseEntity.ok(response);
    }
}
