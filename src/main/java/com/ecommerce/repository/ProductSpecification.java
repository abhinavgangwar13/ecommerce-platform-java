package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specification builder for Product search and multi-attribute filtering.
 */
public class ProductSpecification {

    public static Specification<Product> filter(
            String search,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search in name, description, or category
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                Predicate categoryMatch = cb.like(cb.lower(root.get("category")), searchPattern);
                predicates.add(cb.or(nameMatch, descMatch, categoryMatch));
            }

            // Filter by category (case-insensitive)
            if (category != null && !category.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }

            // Filter by minimum price
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            // Filter by maximum price
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // Filter by in-stock availability
            if (inStock != null) {
                if (inStock) {
                    predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
                } else {
                    predicates.add(cb.equal(root.get("stockQuantity"), 0));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
