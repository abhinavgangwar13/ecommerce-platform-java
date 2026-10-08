package com.ecommerce.dto;

import com.ecommerce.entity.Product;
import com.ecommerce.enums.Role;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO representing Seller Dashboard:
 * - Seller Profile (name, email, role)
 * - Simple Statistics (totalProducts, totalStock, totalOrders, totalSalesAmount)
 * - Seller Products (list of products owned by this seller)
 */
public class SellerDashboardResponse {

    // Seller Information
    private String name;
    private String email;
    private Role role;

    // Simple Statistics
    private Integer totalProducts;
    private Integer totalStock;
    private Long totalOrders;
    private BigDecimal totalSalesAmount;

    // Seller Products
    private List<SellerProductSummary> products;

    public SellerDashboardResponse() {
    }

    public SellerDashboardResponse(String name, String email, Role role,
                                   Integer totalProducts, Integer totalStock,
                                   Long totalOrders, BigDecimal totalSalesAmount,
                                   List<SellerProductSummary> products) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.totalProducts = totalProducts;
        this.totalStock = totalStock;
        this.totalOrders = totalOrders;
        this.totalSalesAmount = totalSalesAmount;
        this.products = products;
    }

    /**
     * DTO representing a product summary in the seller's dashboard.
     */
    public static class SellerProductSummary {
        private Long id;
        private Long productId;
        private String name;
        private String category;
        private BigDecimal price;
        private Integer stockQuantity;
        private String description;
        private String imageUrl;

        public SellerProductSummary() {
        }

        public SellerProductSummary(Long id, String name, String category, BigDecimal price,
                                    Integer stockQuantity, String description, String imageUrl) {
            this.id = id;
            this.productId = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.stockQuantity = stockQuantity;
            this.description = description;
            this.imageUrl = imageUrl;
        }

        public static SellerProductSummary fromEntity(Product product) {
            return new SellerProductSummary(
                    product.getId(),
                    product.getName(),
                    product.getCategory(),
                    product.getPrice(),
                    product.getStockQuantity(),
                    product.getDescription(),
                    product.getImageUrl()
            );
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
            this.productId = id;
        }

        public Long getProductId() {
            return productId != null ? productId : id;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
            this.id = productId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public Integer getStockQuantity() {
            return stockQuantity;
        }

        public void setStockQuantity(Integer stockQuantity) {
            this.stockQuantity = stockQuantity;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
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

    public Integer getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(Integer totalProducts) {
        this.totalProducts = totalProducts;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public void setTotalSalesAmount(BigDecimal totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
    }

    public List<SellerProductSummary> getProducts() {
        return products;
    }

    public void setProducts(List<SellerProductSummary> products) {
        this.products = products;
    }
}
