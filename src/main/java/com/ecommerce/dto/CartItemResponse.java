package com.ecommerce.dto;

import com.ecommerce.entity.CartItem;
import java.math.BigDecimal;

/**
 * DTO representing an item in the buyer's cart with calculated subtotal.
 */
public class CartItemResponse {

    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private String imageUrl;
    private Integer availableStock;

    public CartItemResponse() {
    }

    public CartItemResponse(Long productId, String productName, BigDecimal productPrice,
                            Integer quantity, BigDecimal subtotal, String imageUrl, Integer availableStock) {
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.imageUrl = imageUrl;
        this.availableStock = availableStock;
    }

    public static CartItemResponse fromEntity(CartItem item) {
        BigDecimal price = item.getProduct().getPrice();
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));

        return new CartItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                price,
                item.getQuantity(),
                subtotal,
                item.getProduct().getImageUrl(),
                item.getProduct().getStockQuantity()
        );
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }
}
