package com.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller to verify and test Role-Based Access Control (RBAC).
 */
@RestController
@RequestMapping("/api/test")
public class RoleTestController {

    @GetMapping("/buyer")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Map<String, String>> buyerAccess() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Access Granted: BUYER role verified!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Map<String, String>> sellerAccess() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Access Granted: SELLER role verified!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminAccess() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Access Granted: ADMIN role verified!");
        return ResponseEntity.ok(response);
    }
}
