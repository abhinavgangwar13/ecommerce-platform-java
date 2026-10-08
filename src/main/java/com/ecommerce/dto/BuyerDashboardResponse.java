package com.ecommerce.dto;

import com.ecommerce.entity.Order;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing the Buyer Dashboard summary:
 * - Buyer Profile (name, email, role)
 * - Statistics (total orders, total items purchased, total amount spent)
 * - Recent Orders (latest 5 orders with id, orderDate, status, totalAmount)
 */
public class BuyerDashboardResponse {

    // Buyer Information
    private String name;
    private String email;
    private Role role;

    // Simple Statistics
    private Integer totalOrders;
    private Integer totalItemsPurchased;
    private BigDecimal totalAmountSpent;

    // Recent Orders (latest up to 5)
    private List<RecentOrderSummary> recentOrders;

    public BuyerDashboardResponse() {
    }

    public BuyerDashboardResponse(String name, String email, Role role,
                                  Integer totalOrders, Integer totalItemsPurchased,
                                  BigDecimal totalAmountSpent, List<RecentOrderSummary> recentOrders) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.totalOrders = totalOrders;
        this.totalItemsPurchased = totalItemsPurchased;
        this.totalAmountSpent = totalAmountSpent;
        this.recentOrders = recentOrders;
    }

    /**
     * DTO representing an order summary in the recent orders list.
     */
    public static class RecentOrderSummary {
        private Long orderId;
        private LocalDateTime orderDate;
        private OrderStatus status;
        private BigDecimal totalAmount;

        public RecentOrderSummary() {
        }

        public RecentOrderSummary(Long orderId, LocalDateTime orderDate, OrderStatus status, BigDecimal totalAmount) {
            this.orderId = orderId;
            this.orderDate = orderDate;
            this.status = status;
            this.totalAmount = totalAmount;
        }

        public static RecentOrderSummary fromEntity(Order order) {
            return new RecentOrderSummary(
                    order.getId(),
                    order.getOrderDate(),
                    order.getStatus(),
                    order.getTotalAmount()
            );
        }

        public Long getId() {
            return orderId;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public LocalDateTime getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(LocalDateTime orderDate) {
            this.orderDate = orderDate;
        }

        public OrderStatus getStatus() {
            return status;
        }

        public void setStatus(OrderStatus status) {
            this.status = status;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Integer getTotalItemsPurchased() {
        return totalItemsPurchased;
    }

    public void setTotalItemsPurchased(Integer totalItemsPurchased) {
        this.totalItemsPurchased = totalItemsPurchased;
    }

    public BigDecimal getTotalAmountSpent() {
        return totalAmountSpent;
    }

    public void setTotalAmountSpent(BigDecimal totalAmountSpent) {
        this.totalAmountSpent = totalAmountSpent;
    }

    public List<RecentOrderSummary> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<RecentOrderSummary> recentOrders) {
        this.recentOrders = recentOrders;
    }
}
