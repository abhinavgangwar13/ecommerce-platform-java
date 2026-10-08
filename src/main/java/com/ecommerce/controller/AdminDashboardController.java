package com.ecommerce.controller;

import com.ecommerce.dto.AdminDashboardResponse;
import com.ecommerce.service.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing Admin Dashboard endpoint.
 * Restricted strictly to authenticated users with ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService adminDashboardService;

    /**
     * Get system-wide dashboard overview, statistics, recent orders, and recent users.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard(Authentication authentication) {
        String email = authentication.getName();
        AdminDashboardResponse response = adminDashboardService.getDashboard(email);
        return ResponseEntity.ok(response);
    }
}
