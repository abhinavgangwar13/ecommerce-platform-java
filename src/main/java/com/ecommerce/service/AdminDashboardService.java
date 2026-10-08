package com.ecommerce.service;

import com.ecommerce.dto.AdminDashboardResponse;
import com.ecommerce.dto.AdminDashboardResponse.AdminRecentOrderSummary;
import com.ecommerce.dto.AdminDashboardResponse.AdminRecentUserSummary;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing Admin Dashboard operations and system-wide statistics computation.
 */
@Service
public class AdminDashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Compute system-wide dashboard metrics for authenticated ADMIN.
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with email: " + adminEmail));

        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can access the admin dashboard.");
        }

        // 1. User statistics
        long totalUsers = userRepository.count();
        long totalBuyers = userRepository.countByRole(Role.BUYER);
        long totalSellers = userRepository.countByRole(Role.SELLER);
        long totalAdmins = userRepository.countByRole(Role.ADMIN);

        // 2. Product statistics
        long totalProducts = productRepository.count();
        List<Product> allProducts = productRepository.findAll();
        long totalStock = allProducts.stream()
                .mapToLong(Product::getStockQuantity)
                .sum();

        // 3. Order statistics
        List<Order> allOrders = orderRepository.findAllWithBuyerOrderByOrderDateDesc();
        long totalOrders = allOrders.size();
        BigDecimal totalOrderAmount = allOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Recent orders (latest 5)
        List<AdminRecentOrderSummary> recentOrders = allOrders.stream()
                .limit(5)
                .map(AdminRecentOrderSummary::fromEntity)
                .collect(Collectors.toList());

        // 5. Recent registered users (latest 5)
        List<User> allUsers = userRepository.findAllByOrderByCreatedAtDesc();
        List<AdminRecentUserSummary> recentUsers = allUsers.stream()
                .limit(5)
                .map(AdminRecentUserSummary::fromEntity)
                .collect(Collectors.toList());

        return new AdminDashboardResponse(
                totalUsers,
                totalBuyers,
                totalSellers,
                totalAdmins,
                totalProducts,
                totalStock,
                totalOrders,
                totalOrderAmount,
                recentOrders,
                recentUsers
        );
    }
}
