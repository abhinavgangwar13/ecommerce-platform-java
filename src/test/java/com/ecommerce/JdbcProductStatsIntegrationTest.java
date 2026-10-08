package com.ecommerce;

import com.ecommerce.dao.ProductStatsJdbcDao;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.ProductStatsJdbcResponse;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class JdbcProductStatsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductStatsJdbcDao productStatsJdbcDao;

    private String adminToken;
    private String sellerToken;
    private String buyerToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken("admin@demo.com", "admin123");
        sellerToken = obtainToken("seller@demo.com", "seller123");
        buyerToken = obtainToken("buyer@demo.com", "buyer123");
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
    @DisplayName("1. Direct JDBC DAO test executes Connection, PreparedStatement, and ResultSet successfully")
    void testJdbcDaoDirectExecution() {
        int count = productStatsJdbcDao.countProductsWithStockGreaterThan(0);
        assertTrue(count >= 0, "JDBC count query should return non-negative integer");

        ProductStatsJdbcResponse stats = productStatsJdbcDao.getProductStatistics(0);
        assertNotNull(stats, "JDBC statistics response should not be null");
        assertNotNull(stats.getTotalProducts(), "Total products should not be null");
        assertNotNull(stats.getTotalStock(), "Total stock should not be null");
        assertNotNull(stats.getAveragePrice(), "Average price should not be null");
        assertEquals("Explicit JDBC (java.sql.Connection, PreparedStatement, ResultSet)", stats.getExecutionMechanism());
    }

    @Test
    @DisplayName("2. ADMIN can access GET /api/admin/jdbc/product-stats and receive JDBC metrics")
    void testAdminCanAccessJdbcEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/jdbc/product-stats")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").isNumber())
                .andExpect(jsonPath("$.totalStock").isNumber())
                .andExpect(jsonPath("$.averagePrice").isNumber())
                .andExpect(jsonPath("$.inStockCount").isNumber())
                .andExpect(jsonPath("$.outOfStockCount").isNumber())
                .andExpect(jsonPath("$.executionMechanism").value("Explicit JDBC (java.sql.Connection, PreparedStatement, ResultSet)"));
    }

    @Test
    @DisplayName("3. BUYER and SELLER are forbidden (403) from accessing admin JDBC endpoint")
    void testNonAdminForbiddenFromJdbcEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/jdbc/product-stats")
                        .header("Authorization", buyerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/jdbc/product-stats")
                        .header("Authorization", sellerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Unauthenticated request to admin JDBC endpoint is rejected")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/admin/jdbc/product-stats"))
                .andExpect(status().isForbidden());
    }
}
