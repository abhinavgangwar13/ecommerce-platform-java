package com.ecommerce.service;

import com.ecommerce.dto.PagedResponse;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductSpecification;
import com.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing product operations, business rules, and role-based permissions.
 */
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a product. Only users with role SELLER are allowed.
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        if (user.getRole() != Role.SELLER) {
            throw new AccessDeniedException("Only sellers can create products.");
        }

        Product product = new Product(
                request.getName().trim(),
                request.getDescription(),
                request.getPrice(),
                request.getStockQuantity(),
                request.getCategory().trim(),
                request.getImageUrl(),
                user
        );

        Product savedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(savedProduct);
    }

    /**
     * Retrieve products with search, multi-attribute filtering, sorting, and pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProducts(
            String search,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            int page,
            int size,
            String sortBy,
            String direction) {

        // Pagination validation
        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero.");
        }
        if (size > 100) {
            size = 100; // Cap to maximum 100 items per page
        }

        // Price range validation
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum price cannot be negative.");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Maximum price cannot be negative.");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price.");
        }

        // Sort validation and normalization
        String sortField = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "createdAt";
        String sortDir = (direction != null && !direction.trim().isEmpty()) ? direction.trim().toLowerCase() : "asc";

        if ("newest".equalsIgnoreCase(sortField)) {
            sortField = "createdAt";
            sortDir = "desc";
        }

        if (!sortField.equals("price") && !sortField.equals("name") && !sortField.equals("createdAt") && !sortField.equals("id")) {
            throw new IllegalArgumentException("Invalid sortBy field: " + sortBy + ". Allowed values: price, name, createdAt, newest.");
        }

        Sort.Direction dir = sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(dir, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Product> spec = ProductSpecification.filter(search, category, minPrice, maxPrice, inStock);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast()
        );
    }

    /**
     * Get all products unpaginated (for backward compatibility).
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get product by ID.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findProductById(id);
        return ProductResponse.fromEntity(product);
    }

    /**
     * Get products belonging to the logged-in seller.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getMyProducts(String userEmail) {
        User seller = getUserByEmail(userEmail);
        return productRepository.findBySellerId(seller.getId()).stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get products by category.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category.trim()).stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search products by keyword (name or category).
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String query) {
        String cleanQuery = query != null ? query.trim() : "";
        return productRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(cleanQuery, cleanQuery).stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update product.
     * Allowed for: Product owner (SELLER) OR any ADMIN.
     */
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, String userEmail) {
        Product product = findProductById(id);
        User user = getUserByEmail(userEmail);

        validateProductOwnershipOrAdmin(product, user, "modify");

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory().trim());
        product.setImageUrl(request.getImageUrl());

        Product updatedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(updatedProduct);
    }

    /**
     * Update stock quantity.
     * Allowed for: Product owner (SELLER) OR any ADMIN.
     */
    @Transactional
    public ProductResponse updateStock(Long id, Integer stockQuantity, String userEmail) {
        if (stockQuantity == null || stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }

        Product product = findProductById(id);
        User user = getUserByEmail(userEmail);

        validateProductOwnershipOrAdmin(product, user, "update stock for");

        product.setStockQuantity(stockQuantity);
        Product updatedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(updatedProduct);
    }

    /**
     * Delete product.
     * Allowed for: Product owner (SELLER) OR any ADMIN.
     */
    @Transactional
    public void deleteProduct(Long id, String userEmail) {
        Product product = findProductById(id);
        User user = getUserByEmail(userEmail);

        validateProductOwnershipOrAdmin(product, user, "delete");

        productRepository.delete(product);
    }

    // Helper methods

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private void validateProductOwnershipOrAdmin(Product product, User user, String action) {
        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = product.getSeller() != null && product.getSeller().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to " + action + " this product.");
        }
    }
}
