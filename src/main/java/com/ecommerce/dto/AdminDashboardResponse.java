package com.ecommerce.dto;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing Admin Dashboard:
 * - System-wide user statistics (totalUsers, totalBuyers, totalSellers, totalAdmins)
 * - Product statistics (totalProducts, totalStock)
 * - Order statistics (totalOrders, totalOrderAmount)
 * - Recent orders (latest 5 orders with buyer info)
 * - Recent users (latest 5 registered users)
 */
public class AdminDashboardResponse {

    // User statistics
    private Long totalUsers;
    private Long totalBuyers;
    private Long totalSellers;
    private Long totalAdmins;

    // Product statistics
    private Long totalProducts;
    private Long totalStock;

    // Order statistics
    private Long totalOrders;
    private BigDecimal totalOrderAmount;

    // Recent data
    private List<AdminRecentOrderSummary> recentOrders;
    private List<AdminRecentUserSummary> recentUsers;

    public AdminDashboardResponse() {
    }

    public AdminDashboardResponse(Long totalUsers, Long totalBuyers, Long totalSellers, Long totalAdmins,
                                  Long totalProducts, Long totalStock,
                                  Long totalOrders, BigDecimal totalOrderAmount,
                                  List<AdminRecentOrderSummary> recentOrders,
                                  List<AdminRecentUserSummary> recentUsers) {
        this.totalUsers = totalUsers;
        this.totalBuyers = totalBuyers;
        this.totalSellers = totalSellers;
        this.totalAdmins = totalAdmins;
        this.totalProducts = totalProducts;
        this.totalStock = totalStock;
        this.totalOrders = totalOrders;
        this.totalOrderAmount = totalOrderAmount;
        this.recentOrders = recentOrders;
        this.recentUsers = recentUsers;
    }

    /**
     * Recent order summary containing buyer details and order amounts.
     */
    public static class AdminRecentOrderSummary {
        private Long id;
        private Long orderId;
        private String buyerName;
        private String buyerEmail;
        private LocalDateTime orderDate;
        private OrderStatus status;
        private BigDecimal totalAmount;

        public AdminRecentOrderSummary() {
        }

        public AdminRecentOrderSummary(Long id, String buyerName, String buyerEmail,
                                       LocalDateTime orderDate, OrderStatus status, BigDecimal totalAmount) {
            this.id = id;
            this.orderId = id;
            this.buyerName = buyerName;
            this.buyerEmail = buyerEmail;
            this.orderDate = orderDate;
            this.status = status;
            this.totalAmount = totalAmount;
        }

        public static AdminRecentOrderSummary fromEntity(Order order) {
            String name = order.getBuyer() != null ? order.getBuyer().getName() : "Unknown";
            String email = order.getBuyer() != null ? order.getBuyer().getEmail() : "Unknown";
            return new AdminRecentOrderSummary(
                    order.getId(),
                    name,
                    email,
                    order.getOrderDate(),
                    order.getStatus(),
                    order.getTotalAmount()
            );
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
            this.orderId = id;
        }

        public Long getOrderId() {
            return orderId != null ? orderId : id;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
            this.id = orderId;
        }

        public String getBuyerName() {
            return buyerName;
        }

        public void setBuyerName(String buyerName) {
            this.buyerName = buyerName;
        }

        public String getBuyerEmail() {
            return buyerEmail;
        }

        public void setBuyerEmail(String buyerEmail) {
            this.buyerEmail = buyerEmail;
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

    /**
     * Recent user summary containing registered user details.
     */
    public static class AdminRecentUserSummary {
        private Long id;
        private Long userId;
        private String name;
        private String email;
        private Role role;

        public AdminRecentUserSummary() {
        }

        public AdminRecentUserSummary(Long id, String name, String email, Role role) {
            this.id = id;
            this.userId = id;
            this.name = name;
            this.email = email;
            this.role = role;
        }

        public static AdminRecentUserSummary fromEntity(User user) {
            return new AdminRecentUserSummary(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole()
            );
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
            this.userId = id;
        }

        public Long getUserId() {
            return userId != null ? userId : id;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
            this.id = userId;
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
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getTotalBuyers() {
        return totalBuyers;
    }

    public void setTotalBuyers(Long totalBuyers) {
        this.totalBuyers = totalBuyers;
    }

    public Long getTotalSellers() {
        return totalSellers;
    }

    public void setTotalSellers(Long totalSellers) {
        this.totalSellers = totalSellers;
    }

    public Long getTotalAdmins() {
        return totalAdmins;
    }

    public void setTotalAdmins(Long totalAdmins) {
        this.totalAdmins = totalAdmins;
    }

    public Long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(Long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public Long getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Long totalStock) {
        this.totalStock = totalStock;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalOrderAmount() {
        return totalOrderAmount;
    }

    public void setTotalOrderAmount(BigDecimal totalOrderAmount) {
        this.totalOrderAmount = totalOrderAmount;
    }

    public List<AdminRecentOrderSummary> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<AdminRecentOrderSummary> recentOrders) {
        this.recentOrders = recentOrders;
    }

    public List<AdminRecentUserSummary> getRecentUsers() {
        return recentUsers;
    }

    public void setRecentUsers(List<AdminRecentUserSummary> recentUsers) {
        this.recentUsers = recentUsers;
    }
}
