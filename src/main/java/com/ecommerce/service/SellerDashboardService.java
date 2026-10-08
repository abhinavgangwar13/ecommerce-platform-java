package com.ecommerce.service;

import com.ecommerce.dto.SellerDashboardResponse;
import com.ecommerce.dto.SellerDashboardResponse.SellerProductSummary;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderItemRepository;
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
 * Service managing Seller Dashboard data retrieval and metrics calculation:
 * - Seller profile information
 * - Total products and cumulative inventory stock
 * - Total distinct orders containing seller's products
 * - Total sales revenue generated from seller's products
 * - List of products belonging to this seller
 */
@Service
public class SellerDashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    /**
     * Retrieve dashboard overview for the authenticated seller.
     */
    @Transactional(readOnly = true)
    public SellerDashboardResponse getDashboard(String sellerEmail) {
        User user = userRepository.findByEmail(sellerEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + sellerEmail));

        if (user.getRole() != Role.SELLER) {
            throw new AccessDeniedException("Only sellers can access the seller dashboard.");
        }

        // 1. Fetch products owned by this seller
        List<Product> products = productRepository.findBySellerId(user.getId());
        int totalProducts = products.size();
        int totalStock = products.stream()
                .mapToInt(Product::getStockQuantity)
                .sum();

        // 2. Fetch order items for this seller's products
        List<OrderItem> orderItems = orderItemRepository.findBySellerId(user.getId());

        // Filter out cancelled orders
        List<OrderItem> activeOrderItems = orderItems.stream()
                .filter(oi -> oi.getOrder() != null && oi.getOrder().getStatus() != OrderStatus.CANCELLED)
                .toList();

        long totalOrders = activeOrderItems.stream()
                .map(oi -> oi.getOrder().getId())
                .distinct()
                .count();

        BigDecimal totalSalesAmount = activeOrderItems.stream()
                .map(oi -> oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Map products to summary responses
        List<SellerProductSummary> productSummaries = products.stream()
                .map(SellerProductSummary::fromEntity)
                .collect(Collectors.toList());

        return new SellerDashboardResponse(
                user.getName(),
                user.getEmail(),
                user.getRole(),
                totalProducts,
                totalStock,
                totalOrders,
                totalSalesAmount,
                productSummaries
        );
    }
}
