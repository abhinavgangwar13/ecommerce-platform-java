package com.ecommerce.controller;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing RESTful shopping cart endpoints strictly for BUYER users.
 */
@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('BUYER')")
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * View current buyer's shopping cart.
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        String email = authentication.getName();
        CartResponse response = cartService.getCart(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Add a product to the cart.
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(@Valid @RequestBody AddToCartRequest request,
                                                      Authentication authentication) {
        String email = authentication.getName();
        CartResponse response = cartService.addItemToCart(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update quantity of a product in the cart.
     */
    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItemQuantity(@PathVariable Long productId,
                                                           @Valid @RequestBody UpdateCartItemRequest request,
                                                           Authentication authentication) {
        String email = authentication.getName();
        CartResponse response = cartService.updateItemQuantity(productId, request.getQuantity(), email);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a product from the cart.
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long productId,
                                                   Authentication authentication) {
        String email = authentication.getName();
        CartResponse response = cartService.removeItem(productId, email);
        return ResponseEntity.ok(response);
    }

    /**
     * Clear all items from the cart.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse> clearCart(Authentication authentication) {
        String email = authentication.getName();
        cartService.clearCart(email);
        return ResponseEntity.ok(new ApiResponse(true, "Cart cleared successfully."));
    }
}
