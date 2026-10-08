package com.ecommerce.service;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service managing buyer shopping cart operations.
 */
@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Retrieve the logged-in buyer's cart.
     */
    @Transactional
    public CartResponse getCart(String buyerEmail) {
        Cart cart = getOrCreateCart(buyerEmail);
        return CartResponse.fromEntity(cart);
    }

    /**
     * Add a product to the buyer's cart.
     * If the product already exists in the cart, increases its quantity.
     * Validates available product stock without reducing it.
     */
    @Transactional
    public CartResponse addItemToCart(AddToCartRequest request, String buyerEmail) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Cart cart = getOrCreateCart(buyerEmail);

        // Check if item is already in cart
        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            if (newQuantity > product.getStockQuantity()) {
                throw new IllegalArgumentException("Insufficient stock. Available: " + product.getStockQuantity()
                        + ", requested total: " + newQuantity);
            }
            existingItem.setQuantity(newQuantity);
        } else {
            if (request.getQuantity() > product.getStockQuantity()) {
                throw new IllegalArgumentException("Insufficient stock. Available: " + product.getStockQuantity()
                        + ", requested: " + request.getQuantity());
            }
            CartItem newItem = new CartItem(cart, product, request.getQuantity());
            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return CartResponse.fromEntity(savedCart);
    }

    /**
     * Update quantity of a product in the cart.
     */
    @Transactional
    public CartResponse updateItemQuantity(Long productId, Integer quantity, String buyerEmail) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }

        Cart cart = getOrCreateCart(buyerEmail);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (quantity > product.getStockQuantity()) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + product.getStockQuantity()
                    + ", requested: " + quantity);
        }

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in cart."));

        item.setQuantity(quantity);
        Cart savedCart = cartRepository.save(cart);
        return CartResponse.fromEntity(savedCart);
    }

    /**
     * Remove a single product item from the cart.
     */
    @Transactional
    public CartResponse removeItem(Long productId, String buyerEmail) {
        Cart cart = getOrCreateCart(buyerEmail);

        boolean removed = cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        if (!removed) {
            throw new ResourceNotFoundException("Product not found in cart.");
        }

        Cart savedCart = cartRepository.save(cart);
        return CartResponse.fromEntity(savedCart);
    }

    /**
     * Clear all items from the cart.
     */
    @Transactional
    public CartResponse clearCart(String buyerEmail) {
        Cart cart = getOrCreateCart(buyerEmail);
        cart.getItems().clear();
        Cart savedCart = cartRepository.save(cart);
        return CartResponse.fromEntity(savedCart);
    }

    /**
     * Helper to retrieve or lazily create a cart for the authenticated buyer.
     */
    private Cart getOrCreateCart(String buyerEmail) {
        User user = userRepository.findByEmail(buyerEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + buyerEmail));

        if (user.getRole() != Role.BUYER) {
            throw new AccessDeniedException("Only buyers can access shopping cart.");
        }

        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart(user);
                    return cartRepository.save(newCart);
                });
    }
}
