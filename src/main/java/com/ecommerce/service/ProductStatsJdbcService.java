package com.ecommerce.service;

import com.ecommerce.dao.ProductStatsJdbcDao;
import com.ecommerce.dto.ProductStatsJdbcResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service providing product analytics powered by the explicit JDBC DAO implementation.
 */
@Service
public class ProductStatsJdbcService {

    private final ProductStatsJdbcDao productStatsJdbcDao;

    @Autowired
    public ProductStatsJdbcService(ProductStatsJdbcDao productStatsJdbcDao) {
        this.productStatsJdbcDao = productStatsJdbcDao;
    }

    /**
     * Retrieve aggregated product statistics via explicit JDBC query execution.
     *
     * @param stockThreshold Minimum stock quantity for in-stock filtering
     * @return ProductStatsJdbcResponse with calculated metrics
     */
    public ProductStatsJdbcResponse getProductStatistics(int stockThreshold) {
        if (stockThreshold < 0) {
            throw new IllegalArgumentException("Stock threshold cannot be negative.");
        }
        return productStatsJdbcDao.getProductStatistics(stockThreshold);
    }

    /**
     * Count products having stock strictly greater than minStock via explicit JDBC.
     *
     * @param minStock Minimum stock value
     * @return Count of products matching criterion
     */
    public int countProductsWithStockGreaterThan(int minStock) {
        if (minStock < 0) {
            throw new IllegalArgumentException("Minimum stock cannot be negative.");
        }
        return productStatsJdbcDao.countProductsWithStockGreaterThan(minStock);
    }
}
