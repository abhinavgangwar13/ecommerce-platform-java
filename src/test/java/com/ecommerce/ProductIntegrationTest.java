package com.ecommerce;

import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.StockUpdateRequest;
import com.ecommerce.enums.Role;
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
public class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private String adminToken;
    private String seller1Token;
    private String seller2Token;
    private String buyerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate ADMIN
        adminToken = obtainToken("admin@demo.com", "admin123");

        // Authenticate SELLER 1
        seller1Token = obtainToken("seller@demo.com", "seller123");

        // Register or login SELLER 2
        String seller2Email = "seller2@demo.com";
        RegisterRequest registerSeller2 = new RegisterRequest("Second Seller", seller2Email, "seller123", Role.SELLER);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerSeller2)));
        seller2Token = obtainToken(seller2Email, "seller123");

        // Authenticate BUYER
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
    @DisplayName("SELLER can create a product, persists to ecommerce_db")
    void testSellerCanCreateProduct() throws Exception {
        ProductRequest request = new ProductRequest(
                "Gaming Laptop",
                "High performance laptop with RTX 4080",
                new BigDecimal("1499.99"),
                25,
                "Electronics",
                "https://example.com/laptop.png"
        );

        MvcResult result = mockMvc.perform(post("/api/products")
                        .header("Authorization", seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Gaming Laptop"))
                .andExpect(jsonPath("$.price").value(1499.99))
                .andExpect(jsonPath("$.stockQuantity").value(25))
                .andExpect(jsonPath("$.category").value("Electronics"))
                .andExpect(jsonPath("$.sellerEmail").value("seller@demo.com"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long createdId = json.get("id").asLong();

        // Verify in DB
        assertTrue(productRepository.existsById(createdId));
    }

    @Test
    @DisplayName("BUYER cannot create, update, or delete products (Forbidden)")
    void testBuyerCannotCreateUpdateOrDeleteProducts() throws Exception {
        // 1. First, Seller creates a product
        ProductRequest product = new ProductRequest("Buyer Protection Item", "Desc", new BigDecimal("49.99"), 10, "Home", "http://img.jpg");
        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .header("Authorization", seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andReturn();
        Long productId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Buyer attempts to CREATE a product -> 403 Forbidden
        ProductRequest buyerProduct = new ProductRequest("Illegal Item", "Desc", new BigDecimal("10.00"), 5, "Books", "http://img.jpg");
        mockMvc.perform(post("/api/products")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyerProduct)))
                .andExpect(status().isForbidden());

        // 3. Buyer attempts to UPDATE product -> 403 Forbidden
        mockMvc.perform(put("/api/products/" + productId)
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyerProduct)))
                .andExpect(status().isForbidden());

        // 4. Buyer attempts to PATCH stock -> 403 Forbidden
        StockUpdateRequest stockUpdate = new StockUpdateRequest(99);
        mockMvc.perform(patch("/api/products/" + productId + "/stock")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stockUpdate)))
                .andExpect(status().isForbidden());

        // 5. Buyer attempts to DELETE product -> 403 Forbidden
        mockMvc.perform(delete("/api/products/" + productId)
                        .header("Authorization", buyerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BUYER can view all products, search, and filter by category")
    void testBuyerCanViewProducts() throws Exception {
        // Seller creates a test product
        ProductRequest product = new ProductRequest("Wireless Headphones", "Noise cancelling", new BigDecimal("199.99"), 50, "Audio", "http://img.jpg");
        mockMvc.perform(post("/api/products")
                        .header("Authorization", seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated());

        // Buyer views all products
        mockMvc.perform(get("/api/products").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());

        // Public/Anonymous views all products
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Filter by category
        mockMvc.perform(get("/api/products/category/Audio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Search by query
        mockMvc.perform(get("/api/products/search?query=Headphones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("SELLER can update own product, but CANNOT update another seller's product")
    void testSellerCannotModifyOtherSellersProduct() throws Exception {
        // Seller 1 creates product
        ProductRequest product = new ProductRequest("Seller 1 Special Item", "Original Desc", new BigDecimal("79.99"), 15, "Electronics", "http://img.jpg");
        MvcResult res = mockMvc.perform(post("/api/products")
                        .header("Authorization", seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andReturn();
        Long productId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Seller 1 updates own product -> 200 OK
        ProductRequest updateByOwner = new ProductRequest("Seller 1 Item Updated", "Updated Desc", new BigDecimal("89.99"), 20, "Electronics", "http://img.jpg");
        mockMvc.perform(put("/api/products/" + productId)
                        .header("Authorization", seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateByOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Seller 1 Item Updated"))
                .andExpect(jsonPath("$.price").value(89.99));

        // Seller 2 attempts to UPDATE Seller 1's product -> 403 Forbidden
        ProductRequest updateByOther = new ProductRequest("Hacked Name", "Hacked Desc", new BigDecimal("1.00"), 1, "Electronics", "http://img.jpg");
        mockMvc.perform(put("/api/products/" + productId)
                        .header("Authorization", seller2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateByOther)))
                .andExpect(status().isForbidden());

        // Seller 2 attempts to PATCH stock -> 403 Forbidden
        StockUpdateRequest stockUpdate = new StockUpdateRequest(0);
        mockMvc.perform(patch("/api/products/" + productId + "/stock")
                        .header("Authorization", seller2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stockUpdate)))
                .andExpect(status().isForbidden());

        // Seller 2 attempts to DELETE -> 403 Forbidden
        mockMvc.perform(delete("/api/products/" + productId)
                        .header("Authorization", seller2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN can update and delete any product")
    void testAdminCanUpdateAndDeleteAnyProduct() throws Exception {
        // Seller creates product
        ProductRequest product = new ProductRequest("Item For Admin Test", "Desc", new BigDecimal("25.00"), 10, "Kitchen", "http://img.jpg");
        MvcResult res = mockMvc.perform(post("/api/products")
                        .header("Authorization", seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andReturn();
        Long productId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Admin updates product
        ProductRequest adminUpdate = new ProductRequest("Admin Overridden Item", "Admin Desc", new BigDecimal("35.00"), 12, "Kitchen", "http://img.jpg");
        mockMvc.perform(put("/api/products/" + productId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin Overridden Item"));

        // Admin updates stock
        StockUpdateRequest stockUpdate = new StockUpdateRequest(100);
        mockMvc.perform(patch("/api/products/" + productId + "/stock")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stockUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(100));

        // Admin deletes product
        mockMvc.perform(delete("/api/products/" + productId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify product no longer exists in DB
        assertFalse(productRepository.existsById(productId));
    }

    @Test
    @DisplayName("Validation constraints reject invalid product payloads")
    void testProductValidationConstraints() throws Exception {
        // 1. Blank name
        ProductRequest blankName = new ProductRequest("", "Desc", new BigDecimal("10.00"), 5, "Cat", "http://img.jpg");
        mockMvc.perform(post("/api/products")
                        .header("Authorization", seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").isNotEmpty());

        // 2. Price <= 0
        ProductRequest zeroPrice = new ProductRequest("Zero Price", "Desc", new BigDecimal("0.00"), 5, "Cat", "http://img.jpg");
        mockMvc.perform(post("/api/products")
                        .header("Authorization", seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroPrice)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.price").isNotEmpty());

        // 3. Negative stock
        ProductRequest negStock = new ProductRequest("Neg Stock", "Desc", new BigDecimal("10.00"), -5, "Cat", "http://img.jpg");
        mockMvc.perform(post("/api/products")
                        .header("Authorization", seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negStock)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.stockQuantity").isNotEmpty());

        // 4. Blank category
        ProductRequest blankCat = new ProductRequest("Valid Name", "Desc", new BigDecimal("10.00"), 5, "", "http://img.jpg");
        mockMvc.perform(post("/api/products")
                        .header("Authorization", seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankCat)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.category").isNotEmpty());
    }

    @Test
    @DisplayName("SELLER can view own products; BUYER is blocked from seller endpoint")
    void testSellerCanViewOwnProducts() throws Exception {
        // Seller accesses /api/products/seller/my -> 200 OK
        mockMvc.perform(get("/api/products/seller/my").header("Authorization", seller1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Buyer accesses /api/products/seller/my -> 403 Forbidden
        mockMvc.perform(get("/api/products/seller/my").header("Authorization", buyerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Non-existent product returns 404 Not Found")
    void testProductNotFound() throws Exception {
        mockMvc.perform(get("/api/products/99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
