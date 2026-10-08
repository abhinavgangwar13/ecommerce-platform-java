package com.ecommerce.service;

import com.ecommerce.dto.OrderResponse;
import com.ecommerce.entity.*;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.service.async.OrderNotificationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing order operations:
 * - Placing orders from active buyer carts
 * - Viewing order history and details
 * - Order cancellation with inventory stock restoration
 */
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private OrderNotificationService orderNotificationService;

    /**
     * Place a new order from the authenticated buyer's cart.
     * Validates that:
     * - User is a BUYER
     * - Cart is non-empty
     * - Every product has sufficient stock
     * Deducts stock, calculates totalAmount with BigDecimal, clears cart, and sets status PLACED.
     * All executed within a single transaction.
     */
    @Transactional
    public OrderResponse placeOrder(String buyerEmail) {
        User user = getBuyer(buyerEmail);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cannot place order. Cart is empty."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot place order. Cart is empty.");
        }

        // Phase 1: Validate stock for every item before making any changes
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + cartItem.getProduct().getId()));

            if (cartItem.getQuantity() > product.getStockQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product '" + product.getName()
                        + "'. Available: " + product.getStockQuantity() + ", requested: " + cartItem.getQuantity());
            }
        }

        // Phase 2: Create Order, deduct inventory, and create OrderItems
        Order order = new Order();
        order.setBuyer(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PLACED);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + cartItem.getProduct().getId()));

            // Deduct product stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            // Create OrderItem snapshotting current product unit price
            OrderItem orderItem = new OrderItem(order, product, cartItem.getQuantity(), product.getPrice());
            orderItems.add(orderItem);

            BigDecimal itemSubtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemSubtotal);
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // Phase 3: Clear the buyer's cart
        cart.getItems().clear();
        cartRepository.save(cart);

        // Asynchronous background notification dispatch (Core Java multithreading demonstration)
        if (orderNotificationService != null) {
            orderNotificationService.sendOrderConfirmationAsync(savedOrder.getId(), user.getEmail());
        }

        return OrderResponse.fromEntity(savedOrder);
    }

    /**
     * Retrieve order history for the authenticated buyer, newest first.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getBuyerOrders(String buyerEmail) {
        User user = getBuyer(buyerEmail);
        List<Order> orders = orderRepository.findByBuyerIdOrderByOrderDateDesc(user.getId());
        return orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve single order details by ID for the authenticated buyer.
     * Ensures buyers can only view their own orders.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, String buyerEmail) {
        User user = getBuyer(buyerEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getBuyer().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied: You do not have permission to view this order.");
        }

        return OrderResponse.fromEntity(order);
    }

    /**
     * Cancel an existing order.
     * Allowed only if current status is PLACED.
     * Restores ordered quantities back to product stock and marks status CANCELLED.
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String buyerEmail) {
        User user = getBuyer(buyerEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getBuyer().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied: You can only cancel your own orders.");
        }

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalArgumentException("Only orders with status 'PLACED' can be cancelled. Current status: " + order.getStatus());
        }

        // Restore product stock
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Product product = productRepository.findById(item.getProduct().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + item.getProduct().getId()));

                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        // Asynchronous background notification dispatch (Core Java multithreading demonstration)
        if (orderNotificationService != null) {
            orderNotificationService.sendOrderCancellationAsync(savedOrder.getId(), user.getEmail());
        }

        return OrderResponse.fromEntity(savedOrder);
    }

    /**
     * Helper to authenticate and verify user has BUYER role.
     */
    private User getBuyer(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.getRole() != Role.BUYER) {
            throw new AccessDeniedException("Only buyers can access order operations.");
        }

        return user;
    }
}
