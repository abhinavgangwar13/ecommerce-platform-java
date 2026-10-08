package com.ecommerce.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

/**
 * HealthController provides endpoints to verify:
 * 1. That the Spring Boot server is running.
 * 2. That the MySQL database connection is live and active.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkServer() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Spring Boot Application is running successfully!");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> checkDatabase() {
        Map<String, Object> response = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            response.put("databaseStatus", "CONNECTED");
            response.put("databaseProductName", metaData.getDatabaseProductName());
            response.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            response.put("catalog", connection.getCatalog());
            response.put("message", "Successfully connected to MySQL database!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("databaseStatus", "FAILED");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
