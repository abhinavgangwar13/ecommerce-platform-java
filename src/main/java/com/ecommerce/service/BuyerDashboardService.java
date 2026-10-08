package com.ecommerce.service;

import com.ecommerce.dto.BuyerDashboardResponse;
import com.ecommerce.dto.BuyerDashboardResponse.RecentOrderSummary;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.User;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing buyer dashboard statistics and recent order data retrieval.
 */
@Service
public class BuyerDashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Retrieve the dashboard data for the authenticated buyer.
     * Computes:
     * - Buyer Profile (name, email, role)
     * - Total Orders count
     * - Total Items Purchased count (from non-cancelled orders)
     * - Total Amount Spent (sum of total amounts from non-cancelled orders)
     * - Latest 5 orders
     */
    @Transactional(readOnly = true)
    public BuyerDashboardResponse getDashboard(String buyerEmail) {
        User user = userRepository.findByEmail(buyerEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + buyerEmail));

        if (user.getRole() != Role.BUYER) {
            throw new AccessDeniedException("Only buyers can access the buyer dashboard.");
        }

        // Fetch all orders for this buyer, newest first
        List<Order> allOrders = orderRepository.findByBuyerIdOrderByOrderDateDesc(user.getId());

        int totalOrders = allOrders.size();

        // Filter non-cancelled orders for purchased items and spent calculation
        List<Order> activeOrders = allOrders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .toList();

        int totalItemsPurchased = activeOrders.stream()
                .filter(order -> order.getItems() != null)
                .flatMap(order -> order.getItems().stream())
                .mapToInt(OrderItem::getQuantity)
                .sum();

        BigDecimal totalAmountSpent = activeOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Extract the latest 5 orders for recent orders display
        List<RecentOrderSummary> recentOrders = allOrders.stream()
                .limit(5)
                .map(RecentOrderSummary::fromEntity)
                .collect(Collectors.toList());

        return new BuyerDashboardResponse(
                user.getName(),
                user.getEmail(),
                user.getRole(),
                totalOrders,
                totalItemsPurchased,
                totalAmountSpent,
                recentOrders
        );
    }
}
