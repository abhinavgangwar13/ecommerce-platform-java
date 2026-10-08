package com.ecommerce.controller;

import com.ecommerce.dto.SellerDashboardResponse;
import com.ecommerce.service.SellerDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing Seller Dashboard endpoint.
 * Restricted strictly to authenticated users with SELLER role.
 */
@RestController
@RequestMapping("/api/seller")
@PreAuthorize("hasRole('SELLER')")
public class SellerDashboardController {

    @Autowired
    private SellerDashboardService sellerDashboardService;

    /**
     * Get seller dashboard overview, statistics, and product inventory.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<SellerDashboardResponse> getSellerDashboard(Authentication authentication) {
        String email = authentication.getName();
        SellerDashboardResponse response = sellerDashboardService.getDashboard(email);
        return ResponseEntity.ok(response);
    }
}
