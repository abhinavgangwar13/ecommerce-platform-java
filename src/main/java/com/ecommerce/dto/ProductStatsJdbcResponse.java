package com.ecommerce.dto;

import java.math.BigDecimal;

/**
 * DTO representing product inventory statistics computed via explicit JDBC query execution.
 * Demonstrates explicit database retrieval through java.sql.Connection, PreparedStatement, and ResultSet.
 */
public class ProductStatsJdbcResponse {

    private Long totalProducts;
    private Long totalStock;
    private BigDecimal averagePrice;
    private Long inStockCount;
    private Long outOfStockCount;
    private Integer lowStockThreshold;
    private String executionMechanism;

    public ProductStatsJdbcResponse() {
    }

    public ProductStatsJdbcResponse(Long totalProducts, Long totalStock, BigDecimal averagePrice,
                                    Long inStockCount, Long outOfStockCount, Integer lowStockThreshold,
                                    String executionMechanism) {
        this.totalProducts = totalProducts;
        this.totalStock = totalStock;
        this.averagePrice = averagePrice;
        this.inStockCount = inStockCount;
        this.outOfStockCount = outOfStockCount;
        this.lowStockThreshold = lowStockThreshold;
        this.executionMechanism = executionMechanism;
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

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    public Long getInStockCount() {
        return inStockCount;
    }

    public void setInStockCount(Long inStockCount) {
        this.inStockCount = inStockCount;
    }

    public Long getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(Long outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getExecutionMechanism() {
        return executionMechanism;
    }

    public void setExecutionMechanism(String executionMechanism) {
        this.executionMechanism = executionMechanism;
    }
}
