package com.ecommerce.dao;

import com.ecommerce.dto.ProductStatsJdbcResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object explicitly using JDBC (java.sql.Connection, PreparedStatement, ResultSet)
 * to query MySQL ecommerce_db.
 * Demonstrates explicit JDBC code for academic/rubric verification alongside JPA/Hibernate.
 */
@Repository
public class ProductStatsJdbcDao {

    private final DataSource dataSource;

    @Autowired
    public ProductStatsJdbcDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Count products where stock_quantity is strictly greater than the given threshold.
     * Demonstrates: Connection, PreparedStatement with parameter '?', ResultSet, and try-with-resources.
     */
    public int countProductsWithStockGreaterThan(int minStock) {
        String sql = "SELECT COUNT(*) FROM products WHERE stock_quantity > ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, minStock);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing explicit JDBC count query: " + sql, e);
        }

        return 0;
    }

    /**
     * Retrieve aggregated product inventory metrics using an explicit parameterized JDBC query.
     * Demonstrates: Connection, PreparedStatement with positional parameter '?', ResultSet column extraction,
     * and try-with-resources.
     */
    public ProductStatsJdbcResponse getProductStatistics(int stockThreshold) {
        String sql = "SELECT " +
                     "  COUNT(*) AS total_products, " +
                     "  COALESCE(SUM(stock_quantity), 0) AS total_stock, " +
                     "  COALESCE(AVG(price), 0.0) AS avg_price, " +
                     "  SUM(CASE WHEN stock_quantity > ? THEN 1 ELSE 0 END) AS in_stock_count, " +
                     "  SUM(CASE WHEN stock_quantity = 0 THEN 1 ELSE 0 END) AS out_of_stock_count " +
                     "FROM products";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, stockThreshold);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long totalProducts = rs.getLong("total_products");
                    long totalStock = rs.getLong("total_stock");
                    BigDecimal avgPrice = rs.getBigDecimal("avg_price");
                    long inStock = rs.getLong("in_stock_count");
                    long outOfStock = rs.getLong("out_of_stock_count");

                    if (avgPrice != null) {
                        avgPrice = avgPrice.setScale(2, RoundingMode.HALF_UP);
                    } else {
                        avgPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    }

                    return new ProductStatsJdbcResponse(
                            totalProducts,
                            totalStock,
                            avgPrice,
                            inStock,
                            outOfStock,
                            stockThreshold,
                            "Explicit JDBC (java.sql.Connection, PreparedStatement, ResultSet)"
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing explicit JDBC aggregation query", e);
        }

        return new ProductStatsJdbcResponse(
                0L,
                0L,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                0L,
                0L,
                stockThreshold,
                "Explicit JDBC (java.sql.Connection, PreparedStatement, ResultSet)"
        );
    }
}
