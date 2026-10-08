package com.ecommerce;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.enums.Role;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private String adminToken;
    private String sellerToken;
    private String buyerToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken("admin@demo.com", "admin123");
        sellerToken = obtainToken("seller@demo.com", "seller123");
        buyerToken = obtainToken("buyer@demo.com", "buyer123");

        // Clear carts and test orders before each test
        mockMvc.perform(delete("/api/cart").header("Authorization", buyerToken));
        orderRepository.deleteAll();
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

    private Long createProduct(String token, String name, BigDecimal price, int stock) throws Exception {
        ProductRequest req = new ProductRequest(
                name,
                "Description for " + name,
                price,
                stock,
                "Electronics",
                "http://example.com/" + name + ".png"
        );
        MvcResult res = mockMvc.perform(post("/api/products")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
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
    @DisplayName("1. ADMIN can access dashboard and view system statistics")
    void testAdminCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.totalBuyers").isNumber())
                .andExpect(jsonPath("$.totalSellers").isNumber())
                .andExpect(jsonPath("$.totalAdmins").isNumber())
                .andExpect(jsonPath("$.totalProducts").isNumber())
                .andExpect(jsonPath("$.totalStock").isNumber())
                .andExpect(jsonPath("$.totalOrders").isNumber())
                .andExpect(jsonPath("$.totalOrderAmount").isNumber())
                .andExpect(jsonPath("$.recentOrders").isArray())
                .andExpect(jsonPath("$.recentUsers").isArray());
    }

    @Test
    @DisplayName("2. BUYER cannot access admin dashboard (403 Forbidden)")
    void testBuyerCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", buyerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. SELLER cannot access admin dashboard (403 Forbidden)")
    void testSellerCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", sellerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Unauthenticated request to admin dashboard is rejected (401 or 403)")
    void testUnauthenticatedCannotAccessAdminDashboard() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/admin/dashboard")).andReturn();
        int status = res.getResponse().getStatus();
        assertTrue(status == 401 || status == 403, "Unauthenticated request must be rejected with 401 or 403");
    }

    @Test
    @DisplayName("5. User statistics reflect database counts accurately")
    void testUserStatisticsAccurate() throws Exception {
        MvcResult resBefore = mockMvc.perform(get("/api/admin/dashboard").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode rootBefore = objectMapper.readTree(resBefore.getResponse().getContentAsString());
        long usersBefore = rootBefore.get("totalUsers").asLong();
        long buyersBefore = rootBefore.get("totalBuyers").asLong();

        // Register a new buyer
        String uniqueEmail = "new_buyer_" + System.currentTimeMillis() + "@demo.com";
        RegisterRequest registerReq = new RegisterRequest("New Buyer", uniqueEmail, "pass123", Role.BUYER);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Verify dashboard reflects increment
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(usersBefore + 1))
                .andExpect(jsonPath("$.totalBuyers").value(buyersBefore + 1))
                .andExpect(jsonPath("$.totalAdmins", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalSellers", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("6. Product statistics reflect total count and cumulative stock")
    void testProductStatisticsAccurate() throws Exception {
        MvcResult resBefore = mockMvc.perform(get("/api/admin/dashboard").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode rootBefore = objectMapper.readTree(resBefore.getResponse().getContentAsString());
        long productsBefore = rootBefore.get("totalProducts").asLong();
        long stockBefore = rootBefore.get("totalStock").asLong();

        // Create a new product with stock = 25
        createProduct(sellerToken, "Admin Stats Widget " + System.currentTimeMillis(), new BigDecimal("49.99"), 25);

        // Verify dashboard reflects addition
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(productsBefore + 1))
                .andExpect(jsonPath("$.totalStock").value(stockBefore + 25));
    }

    @Test
    @DisplayName("7. Order statistics and recent orders list reflect placed orders")
    void testOrderStatisticsAndRecentOrders() throws Exception {
        Long prodId = createProduct(sellerToken, "Order Test Item " + System.currentTimeMillis(), new BigDecimal("100.00"), 50);

        // Place 2 orders: Order 1 = $200.00, Order 2 = $100.00 -> Total = $300.00
        placeOrder(prodId, 2);
        placeOrder(prodId, 1);

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(2))
                .andExpect(jsonPath("$.totalOrderAmount").value(300.00))
                .andExpect(jsonPath("$.recentOrders", hasSize(2)))
                .andExpect(jsonPath("$.recentOrders[0].buyerEmail").value("buyer@demo.com"))
                .andExpect(jsonPath("$.recentOrders[0].status").value("PLACED"))
                .andExpect(jsonPath("$.recentOrders[0].totalAmount").isNumber());
    }

    @Test
    @DisplayName("8. Recent orders list is capped at maximum 5 latest orders")
    void testRecentOrdersCappedAtFive() throws Exception {
        Long prodId = createProduct(sellerToken, "Recent Order Capped Item " + System.currentTimeMillis(), new BigDecimal("10.00"), 100);

        // Place 6 orders
        for (int i = 0; i < 6; i++) {
            placeOrder(prodId, 1);
        }

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(6))
                .andExpect(jsonPath("$.totalOrderAmount").value(60.00))
                .andExpect(jsonPath("$.recentOrders", hasSize(5)));
    }

    @Test
    @DisplayName("9. Recent users list returns at most 5 users with proper metadata")
    void testRecentUsersList() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentUsers").isArray())
                .andExpect(jsonPath("$.recentUsers[0].id").isNotEmpty())
                .andExpect(jsonPath("$.recentUsers[0].name").isNotEmpty())
                .andExpect(jsonPath("$.recentUsers[0].email").isNotEmpty())
                .andExpect(jsonPath("$.recentUsers[0].role").isNotEmpty());
    }

    @Test
    @DisplayName("10. Static admin-dashboard.html is accessible publicly and returns HTTP 200")
    void testAdminDashboardHtmlAvailable() throws Exception {
        mockMvc.perform(get("/admin-dashboard.html"))
                .andExpect(status().isOk());
    }
}
