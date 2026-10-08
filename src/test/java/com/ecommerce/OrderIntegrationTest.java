package com.ecommerce;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.Role;
import com.ecommerce.repository.OrderRepository;
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
public class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private String buyerToken;
    private String buyer2Token;
    private String sellerToken;
    private String adminToken;

    private Long productId1;
    private Long productId2;

    @BeforeEach
    void setUp() throws Exception {
        buyerToken = obtainToken("buyer@demo.com", "buyer123");
        sellerToken = obtainToken("seller@demo.com", "seller123");
        adminToken = obtainToken("admin@demo.com", "admin123");

        // Obtain or register a secondary buyer for authorization isolation tests
        buyer2Token = obtainOrRegisterBuyer("buyer2_test@demo.com", "buyer2Pass123", "Buyer Two");

        // Clear carts and orders for buyers so each test starts cleanly
        mockMvc.perform(delete("/api/cart").header("Authorization", buyerToken));
        mockMvc.perform(delete("/api/cart").header("Authorization", buyer2Token));
        orderRepository.deleteAll();

        // Create fresh test products by seller
        // Product 1: $100.00, stock = 10
        ProductRequest p1 = new ProductRequest(
                "Order Test Item 1",
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

        // Product 2: $25.00, stock = 5
        ProductRequest p2 = new ProductRequest(
                "Order Test Item 2",
                "Product description 2",
                new BigDecimal("25.00"),
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

    private String obtainOrRegisterBuyer(String email, String password, String name) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        if (result.getResponse().getStatus() == 200) {
            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            return "Bearer " + root.get("token").asText();
        }

        // Register new buyer if not found
        RegisterRequest registerReq = new RegisterRequest(name, email, password, Role.BUYER);
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root = objectMapper.readTree(regResult.getResponse().getContentAsString());
        return "Bearer " + root.get("token").asText();
    }

    @Test
    @DisplayName("1. Buyer can place an order from a non-empty cart, stock decreases, and cart is cleared")
    void testBuyerCanPlaceOrderSuccess() throws Exception {
        // Add 2 of Product 1 to cart
        AddToCartRequest addReq = new AddToCartRequest(productId1, 2);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        // Place order
        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", buyerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(200.00))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(productId1))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].price").value(100.00))
                .andExpect(jsonPath("$.items[0].subtotal").value(200.00))
                .andReturn();

        // Verify cart is cleared
        mockMvc.perform(get("/api/cart").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));

        // Verify stock in database reduced from 10 to 8
        var updatedProduct = productRepository.findById(productId1).orElseThrow();
        assertEquals(8, updatedProduct.getStockQuantity(), "Stock quantity should be reduced by ordered quantity (10 - 2 = 8)");
    }

    @Test
    @DisplayName("2. Multi-item order: correct quantities, subtotals, total amount, and stock deductions")
    void testMultiItemOrderTotalsAndStock() throws Exception {
        // Add 2 of Product 1 ($100 each -> $200)
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId1, 2))));

        // Add 3 of Product 2 ($25 each -> $75)
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId2, 3))));

        // Place order: total should be $275.00
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", buyerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(275.00))
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.items.length()").value(2));

        // Verify both stock levels reduced
        var p1 = productRepository.findById(productId1).orElseThrow();
        assertEquals(8, p1.getStockQuantity(), "Product 1 stock should decrease by 2 (10 -> 8)");

        var p2 = productRepository.findById(productId2).orElseThrow();
        assertEquals(2, p2.getStockQuantity(), "Product 2 stock should decrease by 3 (5 -> 2)");
    }

    @Test
    @DisplayName("3. Empty cart cannot place an order (400 Bad Request)")
    void testEmptyCartCannotPlaceOrder() throws Exception {
        // Cart is already cleared in setUp
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", buyerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Cannot place order. Cart is empty."));
    }

    @Test
    @DisplayName("4. Insufficient stock rejects order and does not modify product stock")
    void testInsufficientStockRejectsOrder() throws Exception {
        // Add 3 of Product 2 (stock = 5)
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId2, 3))));

        // Directly simulate stock dropping to 2 (e.g. bought by someone else)
        var product2 = productRepository.findById(productId2).orElseThrow();
        product2.setStockQuantity(2);
        productRepository.save(product2);

        // Attempting to place order with cart quantity 3 when stock is 2 should fail
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", buyerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Verify stock remains untouched at 2
        var checkProduct = productRepository.findById(productId2).orElseThrow();
        assertEquals(2, checkProduct.getStockQuantity(), "Stock must not be altered if order placement fails");

        // Cart should still retain the items
        mockMvc.perform(get("/api/cart").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    @DisplayName("5. Buyer can view order history sorted newest first")
    void testBuyerOrderHistory() throws Exception {
        // Place first order
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId1, 1))));
        mockMvc.perform(post("/api/orders").header("Authorization", buyerToken))
                .andExpect(status().isCreated());

        // Place second order
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId2, 1))));
        mockMvc.perform(post("/api/orders").header("Authorization", buyerToken))
                .andExpect(status().isCreated());

        // View order history
        mockMvc.perform(get("/api/orders").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("6. Buyer can view own order details with all line items and subtotals")
    void testBuyerCanViewOwnOrderDetails() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId1, 2))));

        MvcResult result = mockMvc.perform(post("/api/orders").header("Authorization", buyerToken))
                .andExpect(status().isCreated())
                .andReturn();
        Long orderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Retrieve order details
        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(200.00))
                .andExpect(jsonPath("$.items[0].productName").value("Order Test Item 1"))
                .andExpect(jsonPath("$.items[0].price").value(100.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(200.00));
    }

    @Test
    @DisplayName("7. Buyer cannot view another buyer's order (403 Forbidden)")
    void testBuyerCannotViewAnotherBuyerOrder() throws Exception {
        // Buyer 1 places order
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId1, 1))));

        MvcResult result = mockMvc.perform(post("/api/orders").header("Authorization", buyerToken))
                .andExpect(status().isCreated())
                .andReturn();
        Long buyer1OrderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Buyer 2 attempts to view Buyer 1's order
        mockMvc.perform(get("/api/orders/" + buyer1OrderId).header("Authorization", buyer2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("8. Buyer can cancel a PLACED order and product stock is restored")
    void testBuyerCanCancelPlacedOrderAndStockRestored() throws Exception {
        // Stock of productId1 is initially 10
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId1, 3))));

        MvcResult result = mockMvc.perform(post("/api/orders").header("Authorization", buyerToken))
                .andExpect(status().isCreated())
                .andReturn();
        Long orderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Verify stock was reduced to 7
        var productAfterOrder = productRepository.findById(productId1).orElseThrow();
        assertEquals(7, productAfterOrder.getStockQuantity());

        // Cancel order
        mockMvc.perform(put("/api/orders/" + orderId + "/cancel").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Verify stock restored back to 10
        var productAfterCancel = productRepository.findById(productId1).orElseThrow();
        assertEquals(10, productAfterCancel.getStockQuantity(), "Stock must be restored back to 10 after order cancellation");
    }

    @Test
    @DisplayName("9. Cannot cancel an order that is already CANCELLED")
    void testCannotCancelAlreadyCancelledOrder() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId1, 1))));

        MvcResult result = mockMvc.perform(post("/api/orders").header("Authorization", buyerToken))
                .andExpect(status().isCreated())
                .andReturn();
        Long orderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Cancel once -> OK
        mockMvc.perform(put("/api/orders/" + orderId + "/cancel").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Cancel again -> 400 Bad Request
        mockMvc.perform(put("/api/orders/" + orderId + "/cancel").header("Authorization", buyerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("10. Buyer cannot cancel another buyer's order (403 Forbidden)")
    void testBuyerCannotCancelAnotherBuyerOrder() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(productId1, 1))));

        MvcResult result = mockMvc.perform(post("/api/orders").header("Authorization", buyerToken))
                .andExpect(status().isCreated())
                .andReturn();
        Long buyer1OrderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Buyer 2 attempts to cancel Buyer 1's order
        mockMvc.perform(put("/api/orders/" + buyer1OrderId + "/cancel").header("Authorization", buyer2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. Seller and Admin cannot access buyer order endpoints (403 Forbidden)")
    void testSellerCannotAccessOrderEndpoints() throws Exception {
        mockMvc.perform(get("/api/orders").header("Authorization", sellerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders").header("Authorization", sellerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/orders").header("Authorization", adminToken))
                .andExpect(status().isForbidden());
    }
}
