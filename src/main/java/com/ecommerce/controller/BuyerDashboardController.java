package com.ecommerce.controller;

import com.ecommerce.dto.BuyerDashboardResponse;
import com.ecommerce.service.BuyerDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing Buyer Dashboard endpoint.
 * Restricted strictly to authenticated users with BUYER role.
 */
@RestController
@RequestMapping("/api/buyer")
@PreAuthorize("hasRole('BUYER')")
public class BuyerDashboardController {

    @Autowired
    private BuyerDashboardService buyerDashboardService;

    /**
     * Get buyer dashboard overview, statistics, and recent orders.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<BuyerDashboardResponse> getBuyerDashboard(Authentication authentication) {
        String email = authentication.getName();
        BuyerDashboardResponse response = buyerDashboardService.getDashboard(email);
        return ResponseEntity.ok(response);
    }
}
