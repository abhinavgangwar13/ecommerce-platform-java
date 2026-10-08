package com.ecommerce;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.RegisterRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SellerDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private String buyerToken;
    private String sellerToken;
    private String seller2Token;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        buyerToken = obtainToken("buyer@demo.com", "buyer123");
        sellerToken = obtainToken("seller@demo.com", "seller123");
        adminToken = obtainToken("admin@demo.com", "admin123");

        seller2Token = obtainOrRegisterSeller("seller2_test@demo.com", "seller2Pass123", "Seller Two");

        // Clear carts and orders before each test
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

    private String obtainOrRegisterSeller(String email, String password, String name) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        if (result.getResponse().getStatus() == 200) {
            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            return "Bearer " + root.get("token").asText();
        }

        RegisterRequest registerReq = new RegisterRequest(name, email, password, Role.SELLER);
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root = objectMapper.readTree(regResult.getResponse().getContentAsString());
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
    @DisplayName("1. SELLER can access dashboard and view own profile")
    void testSellerCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard").header("Authorization", sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.email").value("seller@demo.com"))
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.totalProducts").isNumber())
                .andExpect(jsonPath("$.totalStock").isNumber())
                .andExpect(jsonPath("$.totalOrders").isNumber())
                .andExpect(jsonPath("$.totalSalesAmount").isNumber());
    }

    @Test
    @DisplayName("2. BUYER cannot access seller dashboard (403 Forbidden)")
    void testBuyerCannotAccessSellerDashboard() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard").header("Authorization", buyerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. ADMIN cannot access seller dashboard (403 Forbidden)")
    void testAdminCannotAccessSellerDashboard() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard").header("Authorization", adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Unauthenticated request to seller dashboard is rejected (401 or 403)")
    void testUnauthenticatedCannotAccessSellerDashboard() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/seller/dashboard")).andReturn();
        int status = res.getResponse().getStatus();
        assertTrue(status == 401 || status == 403, "Unauthenticated request must be rejected with 401 or 403");
    }

    @Test
    @DisplayName("5. Seller only sees their own products and not other sellers' products")
    void testSellerOnlySeesOwnProducts() throws Exception {
        String uniqueP1 = "Seller1 Item " + System.currentTimeMillis();
        String uniqueP2 = "Seller2 Item " + System.currentTimeMillis();

        Long s1ProdId = createProduct(sellerToken, uniqueP1, new BigDecimal("80.00"), 15);
        Long s2ProdId = createProduct(seller2Token, uniqueP2, new BigDecimal("120.00"), 20);

        // Seller 1 dashboard
        MvcResult s1Result = mockMvc.perform(get("/api/seller/dashboard").header("Authorization", sellerToken))
                .andExpect(status().isOk())
                .andReturn();
        String s1Json = s1Result.getResponse().getContentAsString();
        assertTrue(s1Json.contains(uniqueP1), "Seller 1 dashboard must contain Seller 1's product");
        assertTrue(!s1Json.contains(uniqueP2), "Seller 1 dashboard must NOT contain Seller 2's product");

        // Seller 2 dashboard
        MvcResult s2Result = mockMvc.perform(get("/api/seller/dashboard").header("Authorization", seller2Token))
                .andExpect(status().isOk())
                .andReturn();
        String s2Json = s2Result.getResponse().getContentAsString();
        assertTrue(s2Json.contains(uniqueP2), "Seller 2 dashboard must contain Seller 2's product");
        assertTrue(!s2Json.contains(uniqueP1), "Seller 2 dashboard must NOT contain Seller 1's product");
    }

    @Test
    @DisplayName("6. Product count, stock count, orders, and sales revenue calculated correctly")
    void testSalesAndOrderStatisticsCalculatedCorrectly() throws Exception {
        // Create 2 products for Seller 2
        Long p1 = createProduct(seller2Token, "Product S2-A", new BigDecimal("50.00"), 20);
        Long p2 = createProduct(seller2Token, "Product S2-B", new BigDecimal("30.00"), 10);

        // Order 1: 2 units of p1 ($100.00)
        placeOrder(p1, 2);

        // Order 2: 1 unit of p2 ($30.00)
        placeOrder(p2, 1);

        // Check Seller 2 dashboard:
        // totalProducts = 2 (or more if prior run)
        // totalOrders = 2
        // totalSalesAmount = $130.00
        mockMvc.perform(get("/api/seller/dashboard").header("Authorization", seller2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("seller2_test@demo.com"))
                .andExpect(jsonPath("$.totalOrders").value(2))
                .andExpect(jsonPath("$.totalSalesAmount").value(130.00));
    }

    @Test
    @DisplayName("7. Cross-seller sales isolation: only seller's own product sales are credited")
    void testCrossSellerSalesIsolation() throws Exception {
        Long s1Prod = createProduct(sellerToken, "Seller 1 Widget", new BigDecimal("100.00"), 10);
        Long s2Prod = createProduct(seller2Token, "Seller 2 Gadget", new BigDecimal("40.00"), 10);

        // Buyer places single order containing both products
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(s1Prod, 1))));

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddToCartRequest(s2Prod, 2))));

        // Place combined order
        mockMvc.perform(post("/api/orders").header("Authorization", buyerToken))
                .andExpect(status().isCreated());

        // Seller 1 dashboard: 1 order, sales = 100.00 (not 180.00)
        mockMvc.perform(get("/api/seller/dashboard").header("Authorization", sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(1))
                .andExpect(jsonPath("$.totalSalesAmount").value(100.00));

        // Seller 2 dashboard: 1 order, sales = 80.00 (not 180.00)
        mockMvc.perform(get("/api/seller/dashboard").header("Authorization", seller2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(1))
                .andExpect(jsonPath("$.totalSalesAmount").value(80.00));
    }
}
