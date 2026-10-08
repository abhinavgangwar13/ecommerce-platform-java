package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.PagedResponse;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.dto.StockUpdateRequest;
import com.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller exposing RESTful endpoints for Product Management, Browsing, Search, and Filtering.
 * - Public/Buyer: Read-only access to products with search, multi-attribute filtering, sorting, pagination
 * - Seller: Full management of own products (create, view own, update, delete, stock update)
 * - Admin: Full administrative access to any product
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * Create a new product.
     * Restricted to: SELLER
     */
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request,
                                                         Authentication authentication) {
        String email = authentication.getName();
        ProductResponse response = productService.createProduct(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Unified flexible endpoint for product browsing, search, multi-attribute filtering,
     * sorting, and pagination.
     * Available to: Public / BUYER / SELLER / ADMIN
     *
     * @param search     Optional text search in product name, description, or category
     * @param category   Optional category filter
     * @param minPrice   Optional minimum price filter
     * @param maxPrice   Optional maximum price filter
     * @param inStock    Optional boolean filter (true = stockQuantity > 0, false = stockQuantity == 0)
     * @param page       0-indexed page number (default: 0)
     * @param size       Page size (default: 10, max: 100)
     * @param sortBy     Field to sort by: price, name, createdAt, newest (default: createdAt)
     * @param direction  Sort direction: asc or desc (default: desc)
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ProductResponse>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        PagedResponse<ProductResponse> response = productService.getProducts(
                search, category, minPrice, maxPrice, inStock, page, size, sortBy, direction);
        return ResponseEntity.ok(response);
    }

    /**
     * View product by ID.
     * Available to: Public / BUYER / SELLER / ADMIN
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    /**
     * View products belonging to the logged-in seller.
     * Restricted to: SELLER
     */
    @GetMapping("/seller/my")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<ProductResponse>> getMyProducts(Authentication authentication) {
        String email = authentication.getName();
        List<ProductResponse> products = productService.getMyProducts(email);
        return ResponseEntity.ok(products);
    }

    /**
     * Convenience endpoint: View products by category.
     * Delegates to unified getProducts with pagination support.
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<PagedResponse<ProductResponse>> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return getProducts(null, category, null, null, null, page, size, sortBy, direction);
    }

    /**
     * Convenience endpoint: Search products by keyword query.
     * Delegates to unified getProducts with pagination support.
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ProductResponse>> searchProducts(
            @RequestParam("query") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return getProducts(query, null, null, null, null, page, size, sortBy, direction);
    }

    /**
     * Update product details.
     * Restricted to: SELLER (own product) or ADMIN
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
                                                         @Valid @RequestBody ProductRequest request,
                                                         Authentication authentication) {
        String email = authentication.getName();
        ProductResponse response = productService.updateProduct(id, request, email);
        return ResponseEntity.ok(response);
    }

    /**
     * Update stock quantity.
     * Restricted to: SELLER (own product) or ADMIN
     */
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ProductResponse> updateStock(@PathVariable Long id,
                                                       @Valid @RequestBody StockUpdateRequest request,
                                                       Authentication authentication) {
        String email = authentication.getName();
        ProductResponse response = productService.updateStock(id, request.getStockQuantity(), email);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete product.
     * Restricted to: SELLER (own product) or ADMIN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long id,
                                                     Authentication authentication) {
        String email = authentication.getName();
        productService.deleteProduct(id, email);
        return ResponseEntity.ok(new ApiResponse(true, "Product deleted successfully."));
    }
}
