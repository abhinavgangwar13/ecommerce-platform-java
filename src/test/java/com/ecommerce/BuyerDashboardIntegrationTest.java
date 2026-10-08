package com.ecommerce;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.repository.OrderRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BuyerDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

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

        // Clear cart and delete existing test orders so every test starts with clean state
        mockMvc.perform(delete("/api/cart").header("Authorization", buyerToken));
        orderRepository.deleteAll();

        // Create test products by seller
        // Product 1: $100.00, stock = 50
        ProductRequest p1 = new ProductRequest(
                "Dashboard Test Item 1",
                "Product description 1",
                new BigDecimal("100.00"),
                50,
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

        // Product 2: $25.00, stock = 50
        ProductRequest p2 = new ProductRequest(
                "Dashboard Test Item 2",
                "Product description 2",
                new BigDecimal("25.00"),
                50,
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

    private void placeOrder(Long productId, int quantity) throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(productId, quantity);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders").header("Authorization", buyerToken))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("1. BUYER can access dashboard with initial zero metrics")
    void testBuyerCanAccessDashboardEmpty() throws Exception {
        mockMvc.perform(get("/api/buyer/dashboard").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.email").value("buyer@demo.com"))
                .andExpect(jsonPath("$.role").value("BUYER"))
                .andExpect(jsonPath("$.totalOrders").value(0))
                .andExpect(jsonPath("$.totalItemsPurchased").value(0))
                .andExpect(jsonPath("$.totalAmountSpent").value(0))
                .andExpect(jsonPath("$.recentOrders").isArray())
                .andExpect(jsonPath("$.recentOrders", hasSize(0)));
    }

    @Test
    @DisplayName("2. SELLER cannot access buyer dashboard (403 Forbidden)")
    void testSellerCannotAccessBuyerDashboard() throws Exception {
        mockMvc.perform(get("/api/buyer/dashboard").header("Authorization", sellerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. ADMIN cannot access buyer dashboard (403 Forbidden)")
    void testAdminCannotAccessBuyerDashboard() throws Exception {
        mockMvc.perform(get("/api/buyer/dashboard").header("Authorization", adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Unauthenticated request to buyer dashboard is rejected (401 or 403)")
    void testUnauthenticatedCannotAccessBuyerDashboard() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/buyer/dashboard")).andReturn();
        int statusCode = result.getResponse().getStatus();
        assertTrue(statusCode == 401 || statusCode == 403, "Unauthenticated request must be rejected with 401 or 403");
    }

    @Test
    @DisplayName("5. Dashboard statistics are calculated correctly for multiple orders")
    void testDashboardStatisticsCalculatedCorrectly() throws Exception {
        // Order 1: 2 of Product 1 ($100 each -> $200.00)
        placeOrder(productId1, 2);

        // Order 2: 3 of Product 2 ($25 each -> $75.00)
        placeOrder(productId2, 3);

        // Verify dashboard statistics:
        // totalOrders = 2
        // totalItemsPurchased = 5
        // totalAmountSpent = 275.00
        mockMvc.perform(get("/api/buyer/dashboard").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("buyer@demo.com"))
                .andExpect(jsonPath("$.totalOrders").value(2))
                .andExpect(jsonPath("$.totalItemsPurchased").value(5))
                .andExpect(jsonPath("$.totalAmountSpent").value(275.00))
                .andExpect(jsonPath("$.recentOrders", hasSize(2)));
    }

    @Test
    @DisplayName("6. Recent orders returns at most the latest 5 orders sorted newest first")
    void testRecentOrdersLimitsToLatestFive() throws Exception {
        // Place 6 orders
        for (int i = 1; i <= 6; i++) {
            placeOrder(productId2, 1);
        }

        mockMvc.perform(get("/api/buyer/dashboard").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(6))
                .andExpect(jsonPath("$.totalItemsPurchased").value(6))
                .andExpect(jsonPath("$.totalAmountSpent").value(150.00))
                // recentOrders must be capped at 5
                .andExpect(jsonPath("$.recentOrders", hasSize(5)))
                .andExpect(jsonPath("$.recentOrders[0].id").isNotEmpty())
                .andExpect(jsonPath("$.recentOrders[0].orderDate").isNotEmpty())
                .andExpect(jsonPath("$.recentOrders[0].status").value("PLACED"))
                .andExpect(jsonPath("$.recentOrders[0].totalAmount").value(25.00));
    }

    @Test
    @DisplayName("7. Cancelling an order updates dashboard metrics and shows CANCELLED in recent orders")
    void testCancelledOrderUpdatesMetrics() throws Exception {
        // Place order 1: 2 items of Product 1 ($200.00)
        AddToCartRequest addReq = new AddToCartRequest(productId1, 2);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        MvcResult orderRes = mockMvc.perform(post("/api/orders").header("Authorization", buyerToken))
                .andExpect(status().isCreated())
                .andReturn();
        Long orderId = objectMapper.readTree(orderRes.getResponse().getContentAsString()).get("id").asLong();

        // Cancel order 1
        mockMvc.perform(put("/api/orders/" + orderId + "/cancel").header("Authorization", buyerToken))
                .andExpect(status().isOk());

        // Verify dashboard reflects cancelled status:
        // totalOrders is still 1 (order history record exists)
        // totalItemsPurchased is 0 (refunded/restored)
        // totalAmountSpent is 0.00
        // recentOrders has 1 order with status CANCELLED
        mockMvc.perform(get("/api/buyer/dashboard").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(1))
                .andExpect(jsonPath("$.totalItemsPurchased").value(0))
                .andExpect(jsonPath("$.totalAmountSpent").value(0))
                .andExpect(jsonPath("$.recentOrders[0].id").value(orderId))
                .andExpect(jsonPath("$.recentOrders[0].status").value("CANCELLED"));
    }
}
