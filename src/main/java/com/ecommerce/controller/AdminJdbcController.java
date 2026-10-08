package com.ecommerce.controller;

import com.ecommerce.dto.ProductStatsJdbcResponse;
import com.ecommerce.service.ProductStatsJdbcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing explicit JDBC-driven product analytics for ADMIN users.
 * Demonstrates integration of explicit java.sql.Connection, PreparedStatement, and ResultSet
 * into the REST API layer of the Spring Boot application.
 */
@RestController
@RequestMapping("/api/admin/jdbc")
@PreAuthorize("hasRole('ADMIN')")
public class AdminJdbcController {

    @Autowired
    private ProductStatsJdbcService productStatsJdbcService;

    /**
     * Retrieve product metrics computed via explicit JDBC query execution.
     * Accessible exclusively to ADMIN role.
     *
     * @param minStock Optional minimum stock threshold parameter (default: 0)
     * @return ProductStatsJdbcResponse with aggregated metrics
     */
    @GetMapping("/product-stats")
    public ResponseEntity<ProductStatsJdbcResponse> getProductStats(
            @RequestParam(defaultValue = "0") int minStock) {
        ProductStatsJdbcResponse stats = productStatsJdbcService.getProductStatistics(minStock);
        return ResponseEntity.ok(stats);
    }
}
