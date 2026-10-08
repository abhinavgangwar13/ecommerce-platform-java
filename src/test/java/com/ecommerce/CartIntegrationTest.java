package com.ecommerce;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private String buyerToken;
    private String sellerToken;
    private String adminToken;

    private Long productId1;
    private Long productId2;

    @BeforeEach
    void setUp() throws Exception {
        buyerToken = obtainToken("buyer@demo.com", "buyer123");
        sellerToken = obtainToken("seller@demo.com", "seller123");
        adminToken = obtainToken("admin@demo.com", "admin123");

        // First clear cart for buyer so each test starts fresh
        mockMvc.perform(delete("/api/cart").header("Authorization", buyerToken));

        // Create test products by seller
        // Product 1: $100.00, stock = 10
        ProductRequest p1 = new ProductRequest(
                "Cart Test Item 1",
                "Product description 1",
                new BigDecimal("100.00"),
                10,
                "Electronics",
                "http://example.com/p1.png"
        );
        MvcResult res1 = mockMvc.perform(post("/api/products")
                        .header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p1)))
                .andExpect(status().isCreated())
                .andReturn();
        productId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        // Product 2: $25.50, stock = 5
        ProductRequest p2 = new ProductRequest(
                "Cart Test Item 2",
                "Product description 2",
                new BigDecimal("25.50"),
                5,
                "Books",
                "http://example.com/p2.png"
        );
        MvcResult res2 = mockMvc.perform(post("/api/products")
                        .header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p2)))
                .andExpect(status().isCreated())
                .andReturn();
        productId2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return "Bearer " + root.get("token").asText();
    }

    @Test
    @DisplayName("1. Buyer can add a product to cart and view cart")
    void testBuyerCanAddProductAndViewCart() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(productId1, 2);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartId").isNotEmpty())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalAmount").value(200.00))
                .andExpect(jsonPath("$.items[0].productId").value(productId1))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(200.00));

        // View Cart
        mockMvc.perform(get("/api/cart").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalAmount").value(200.00))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productName").value("Cart Test Item 1"));
    }

    @Test
    @DisplayName("2. Adding the same product twice increases quantity")
    void testAddingSameProductTwiceIncreasesQuantity() throws Exception {
        AddToCartRequest addReq1 = new AddToCartRequest(productId1, 2);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq1)))
                .andExpect(status().isCreated());

        AddToCartRequest addReq2 = new AddToCartRequest(productId1, 3);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.totalAmount").value(500.00))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.items[0].subtotal").value(500.00));
    }

    @Test
    @DisplayName("3. Buyer can update quantity of item in cart")
    void testBuyerCanUpdateQuantity() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(productId1, 2);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        UpdateCartItemRequest updateReq = new UpdateCartItemRequest(4);
        mockMvc.perform(put("/api/cart/items/" + productId1)
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.totalAmount").value(400.00))
                .andExpect(jsonPath("$.items[0].quantity").value(4));
    }

    @Test
    @DisplayName("4. Buyer can remove an item from cart")
    void testBuyerCanRemoveItem() throws Exception {
        // Add two products
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId1, 1))));

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId2, 2))));

        // Remove product 1
        mockMvc.perform(delete("/api/cart/items/" + productId1)
                        .header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalAmount").value(51.00))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(productId2));
    }

    @Test
    @DisplayName("5. Buyer can clear the entire cart")
    void testBuyerCanClearCart() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId1, 2))));

        mockMvc.perform(delete("/api/cart")
                        .header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify cart is now empty
        mockMvc.perform(get("/api/cart").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @DisplayName("6. Quantity cannot be 0 or negative (Validation Error)")
    void testQuantityCannotBeZeroOrNegative() throws Exception {
        AddToCartRequest zeroReq = new AddToCartRequest(productId1, 0);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroReq)))
                .andExpect(status().isBadRequest());

        AddToCartRequest negReq = new AddToCartRequest(productId1, -3);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. Quantity cannot exceed product available stock")
    void testQuantityCannotExceedStock() throws Exception {
        // Product 2 has stock = 5. Attempt to add 6
        AddToCartRequest overStockReq = new AddToCartRequest(productId2, 6);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overStockReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Product stock should not be reduced
        var product = productRepository.findById(productId2).orElseThrow();
        assertEquals(5, product.getStockQuantity(), "Stock must not be reduced when adding to cart");
    }

    @Test
    @DisplayName("8. Multiple items calculation: item subtotal and cart total")
    void testMultipleItemsTotalCalculation() throws Exception {
        // Add 2 of Product 1 ($100.00 each -> $200.00)
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId1, 2))));

        // Add 3 of Product 2 ($25.50 each -> $76.50)
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId2, 3))));

        // Total should be 200.00 + 76.50 = 276.50
        mockMvc.perform(get("/api/cart").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.totalAmount").value(276.50))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("9. Seller and Admin cannot access cart endpoints (Forbidden)")
    void testSellerCannotAccessCart() throws Exception {
        // Seller attempts GET /api/cart -> 403 Forbidden
        mockMvc.perform(get("/api/cart").header("Authorization", sellerToken))
                .andExpect(status().isForbidden());

        // Seller attempts POST /api/cart/items -> 403 Forbidden
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(productId1, 1))))
                .andExpect(status().isForbidden());

        // Admin attempts GET /api/cart -> 403 Forbidden
        mockMvc.perform(get("/api/cart").header("Authorization", adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. Non-existent product returns 404 Not Found")
    void testNonExistentProduct() throws Exception {
        AddToCartRequest notFoundReq = new AddToCartRequest(999999L, 1);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notFoundReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
