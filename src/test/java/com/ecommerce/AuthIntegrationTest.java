package com.ecommerce;

import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.enums.Role;
import com.ecommerce.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Verify Health Endpoints and Database connection")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/health/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.databaseStatus").value("CONNECTED"))
                .andExpect(jsonPath("$.catalog").value("ecommerce_db"));
    }

    @Test
    @DisplayName("Verify Login with Pre-existing Users and Role-Based Authorization")
    void testPreExistingUsersLoginAndRoles() throws Exception {
        // 1. Login as ADMIN
        LoginRequest adminLogin = new LoginRequest("admin@demo.com", "admin123");
        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        String adminToken = "Bearer " + objectMapper.readTree(adminResult.getResponse().getContentAsString()).get("token").asText();

        // 2. Login as SELLER
        LoginRequest sellerLogin = new LoginRequest("seller@demo.com", "seller123");
        MvcResult sellerResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellerLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andReturn();

        String sellerToken = "Bearer " + objectMapper.readTree(sellerResult.getResponse().getContentAsString()).get("token").asText();

        // 3. Login as BUYER
        LoginRequest buyerLogin = new LoginRequest("buyer@demo.com", "buyer123");
        MvcResult buyerResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyerLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("BUYER"))
                .andReturn();

        String buyerToken = "Bearer " + objectMapper.readTree(buyerResult.getResponse().getContentAsString()).get("token").asText();

        // Verify ADMIN access
        mockMvc.perform(get("/api/test/admin").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Access Granted: ADMIN role verified!"));

        // Verify SELLER access
        mockMvc.perform(get("/api/test/seller").header("Authorization", sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Access Granted: SELLER role verified!"));

        // Verify BUYER access
        mockMvc.perform(get("/api/test/buyer").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Access Granted: BUYER role verified!"));

        // Verify Cross-Role Restrictions (BUYER cannot access SELLER or ADMIN)
        mockMvc.perform(get("/api/test/seller").header("Authorization", buyerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/test/admin").header("Authorization", buyerToken))
                .andExpect(status().isForbidden());

        // Verify Cross-Role Restrictions (SELLER cannot access ADMIN)
        mockMvc.perform(get("/api/test/admin").header("Authorization", sellerToken))
                .andExpect(status().isForbidden());

        // Verify /api/auth/me with Buyer Token
        mockMvc.perform(get("/api/auth/me").header("Authorization", buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("buyer@demo.com"))
                .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    @DisplayName("Verify Invalid Login Credentials")
    void testInvalidLogin() throws Exception {
        LoginRequest badPassword = new LoginRequest("buyer@demo.com", "wrongPassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPassword)))
                .andExpect(status().isUnauthorized());

        LoginRequest unknownUser = new LoginRequest("nonexistent@demo.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownUser)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Verify User Registration, Password Hashing, and Storage in DB")
    void testRegistrationFlow() throws Exception {
        String uniqueEmail = "testuser_" + System.currentTimeMillis() + "@demo.com";
        RegisterRequest registerReq = new RegisterRequest("Test User", uniqueEmail, "secretPass123", Role.BUYER);

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(uniqueEmail))
                .andExpect(jsonPath("$.role").value("BUYER"))
                .andReturn();

        // Verify user in Database
        var savedUserOpt = userRepository.findByEmail(uniqueEmail);
        assertTrue(savedUserOpt.isPresent(), "User must be persisted in database");
        var savedUser = savedUserOpt.get();
        assertEquals("Test User", savedUser.getName());
        assertEquals(Role.BUYER, savedUser.getRole());

        // Verify BCrypt hashing (not plain text)
        assertTrue(savedUser.getPassword().startsWith("$2a$") || savedUser.getPassword().startsWith("$2b$"),
                "Password must be stored as BCrypt hash");

        // Verify duplicate registration rejection
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Verify Unauthenticated Access to Protected Endpoints")
    void testUnauthenticatedAccess() throws Exception {
        // Unauthenticated access to /api/auth/me should be 401 Unauthorized
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // Unauthenticated access to /api/test/buyer should be forbidden or unauthorized (401 or 403)
        MvcResult result = mockMvc.perform(get("/api/test/buyer")).andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == 401 || status == 403, "Unauthenticated request must be rejected with 401 or 403");
    }

    @Test
    @DisplayName("Verify Invalid or Tampered JWT Rejection")
    void testInvalidTokenAccess() throws Exception {
        // Request with invalid token to /api/auth/me
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.token.payload"))
                .andExpect(status().isUnauthorized());

        // Request with invalid token to /api/test/buyer
        MvcResult result = mockMvc.perform(get("/api/test/buyer")
                        .header("Authorization", "Bearer forged.token.value"))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == 401 || status == 403, "Forged token must be rejected with 401 or 403");
    }
}
