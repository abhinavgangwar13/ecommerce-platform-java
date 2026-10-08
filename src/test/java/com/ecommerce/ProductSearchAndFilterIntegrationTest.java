package com.ecommerce;

import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.StockUpdateRequest;
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
public class ProductSearchAndFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private String sellerToken;
    private String buyerToken;

    private Long inStockProductId;
    private Long outOfStockProductId;

    @BeforeEach
    void setUp() throws Exception {
        sellerToken = obtainToken("seller@demo.com", "seller123");
        buyerToken = obtainToken("buyer@demo.com", "buyer123");

        // Seed products for testing search, filtering, and sorting
        // Product 1: In stock, Electronics, $299.99
        ProductRequest prod1 = new ProductRequest(
                "Filterable 4K Monitor",
                "Ultra clear UHD display for gaming and productivity",
                new BigDecimal("299.99"),
                15,
                "Electronics",
                "https://example.com/monitor.png"
        );
        MvcResult res1 = mockMvc.perform(post("/api/products")
                        .header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prod1)))
                .andExpect(status().isCreated())
                .andReturn();
        inStockProductId = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        // Product 2: Out of stock (stock = 0), Electronics, $99.99
        ProductRequest prod2 = new ProductRequest(
                "Filterable Bluetooth Speaker",
                "Portable wireless speaker with deep bass",
                new BigDecimal("99.99"),
                0,
                "Electronics",
                "https://example.com/speaker.png"
        );
        MvcResult res2 = mockMvc.perform(post("/api/products")
                        .header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prod2)))
                .andExpect(status().isCreated())
                .andReturn();
        outOfStockProductId = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();

        // Product 3: In stock, Books, $19.99
        ProductRequest prod3 = new ProductRequest(
                "Java Spring Boot Mastery Book",
                "Complete guide to enterprise application development with Spring",
                new BigDecimal("19.99"),
                50,
                "Books",
                "https://example.com/book.png"
        );
        mockMvc.perform(post("/api/products")
                        .header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prod3)))
                .andExpect(status().isCreated());
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
    @DisplayName("1. Get products with pagination (page, size)")
    void testGetProductsWithPagination() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("content").size() <= 2);
    }

    @Test
    @DisplayName("2. Get product by ID (shows full details and seller info)")
    void testGetProductById() throws Exception {
        mockMvc.perform(get("/api/products/" + inStockProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inStockProductId))
                .andExpect(jsonPath("$.name").value("Filterable 4K Monitor"))
                .andExpect(jsonPath("$.price").value(299.99))
                .andExpect(jsonPath("$.stockQuantity").value(15))
                .andExpect(jsonPath("$.category").value("Electronics"))
                .andExpect(jsonPath("$.sellerName").value("John Seller"))
                .andExpect(jsonPath("$.sellerEmail").value("seller@demo.com"));
    }

    @Test
    @DisplayName("3. Search products by name, description, and category (case-insensitive)")
    void testSearchProducts() throws Exception {
        // Search by name
        MvcResult nameResult = mockMvc.perform(get("/api/products?search=monitor"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode nameJson = objectMapper.readTree(nameResult.getResponse().getContentAsString());
        assertTrue(nameJson.get("content").size() >= 1);
        assertTrue(nameJson.get("content").get(0).get("name").asText().toLowerCase().contains("monitor"));

        // Search by description
        MvcResult descResult = mockMvc.perform(get("/api/products?search=wireless"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode descJson = objectMapper.readTree(descResult.getResponse().getContentAsString());
        assertTrue(descJson.get("content").size() >= 1);

        // Search by category
        MvcResult catResult = mockMvc.perform(get("/api/products?search=books"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode catJson = objectMapper.readTree(catResult.getResponse().getContentAsString());
        assertTrue(catJson.get("content").size() >= 1);
    }

    @Test
    @DisplayName("4. Filter products by category")
    void testFilterByCategory() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products?category=Books"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode item : json.get("content")) {
            assertEquals("Books", item.get("category").asText());
        }
    }

    @Test
    @DisplayName("5. Filter products by price range (minPrice, maxPrice)")
    void testFilterByPriceRange() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products?minPrice=50.00&maxPrice=150.00"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("content").size() >= 1);
        for (JsonNode item : json.get("content")) {
            double price = item.get("price").asDouble();
            assertTrue(price >= 50.00 && price <= 150.00, "Price " + price + " must be between 50 and 150");
        }
    }

    @Test
    @DisplayName("6. Filter products by in-stock availability")
    void testFilterByStockAvailability() throws Exception {
        // Filter inStock = true
        MvcResult inStockResult = mockMvc.perform(get("/api/products?inStock=true"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode inStockJson = objectMapper.readTree(inStockResult.getResponse().getContentAsString());
        for (JsonNode item : inStockJson.get("content")) {
            assertTrue(item.get("stockQuantity").asInt() > 0, "Stock must be > 0 when inStock=true");
        }

        // Filter inStock = false
        MvcResult outOfStockResult = mockMvc.perform(get("/api/products?inStock=false"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode outOfStockJson = objectMapper.readTree(outOfStockResult.getResponse().getContentAsString());
        for (JsonNode item : outOfStockJson.get("content")) {
            assertEquals(0, item.get("stockQuantity").asInt(), "Stock must be 0 when inStock=false");
        }
    }

    @Test
    @DisplayName("7. Sort products by price low-to-high, high-to-low, newest, and name")
    void testSorting() throws Exception {
        // Price ASC
        MvcResult priceAscRes = mockMvc.perform(get("/api/products?sortBy=price&direction=asc"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode priceAscJson = objectMapper.readTree(priceAscRes.getResponse().getContentAsString());
        double prevPrice = -1.0;
        for (JsonNode item : priceAscJson.get("content")) {
            double price = item.get("price").asDouble();
            if (prevPrice >= 0) {
                assertTrue(price >= prevPrice, "Expected ascending price order");
            }
            prevPrice = price;
        }

        // Price DESC
        MvcResult priceDescRes = mockMvc.perform(get("/api/products?sortBy=price&direction=desc"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode priceDescJson = objectMapper.readTree(priceDescRes.getResponse().getContentAsString());
        prevPrice = Double.MAX_VALUE;
        for (JsonNode item : priceDescJson.get("content")) {
            double price = item.get("price").asDouble();
            assertTrue(price <= prevPrice, "Expected descending price order");
            prevPrice = price;
        }

        // Sort by newest
        mockMvc.perform(get("/api/products?sortBy=newest"))
                .andExpect(status().isOk());

        // Sort by name
        mockMvc.perform(get("/api/products?sortBy=name&direction=asc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("8. Combined search, filter, sort, and pagination")
    void testCombinedSearchFilterSortPagination() throws Exception {
        mockMvc.perform(get("/api/products?search=Filterable&category=Electronics&minPrice=50.00&maxPrice=500.00&inStock=true&sortBy=price&direction=asc&page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("9. Validation error handling on invalid query parameters")
    void testParameterValidationHandling() throws Exception {
        // Negative page
        mockMvc.perform(get("/api/products?page=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page index cannot be negative."));

        // Size <= 0
        mockMvc.perform(get("/api/products?size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page size must be greater than zero."));

        // Negative minPrice
        mockMvc.perform(get("/api/products?minPrice=-10.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Minimum price cannot be negative."));

        // Negative maxPrice
        mockMvc.perform(get("/api/products?maxPrice=-5.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Maximum price cannot be negative."));

        // minPrice > maxPrice
        mockMvc.perform(get("/api/products?minPrice=200.00&maxPrice=100.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Minimum price cannot be greater than maximum price."));

        // Invalid sortBy field
        mockMvc.perform(get("/api/products?sortBy=unsupportedColumn"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("10. Public and BUYER role authorization for browsing; mutations forbidden")
    void testBuyerBrowsingAndMutationRestriction() throws Exception {
        // Unauthenticated visitor can browse
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        // Authenticated BUYER can browse
        mockMvc.perform(get("/api/products").header("Authorization", buyerToken))
                .andExpect(status().isOk());

        // Authenticated BUYER CANNOT create product
        ProductRequest forbiddenCreate = new ProductRequest("Buyer Item", "Desc", new BigDecimal("10.00"), 5, "Other", "img.png");
        mockMvc.perform(post("/api/products")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forbiddenCreate)))
                .andExpect(status().isForbidden());
    }
}
