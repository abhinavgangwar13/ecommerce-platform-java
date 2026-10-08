package com.ecommerce;

import com.ecommerce.servlet.ProjectInfoServlet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused integration test for classic HttpServlet integration:
 * Proves that GET /api/servlet/project-info returns HTTP 200 and the expected response.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProjectInfoServletIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Classic HttpServlet GET /api/servlet/project-info returns HTTP 200 and expected description")
    void testProjectInfoServletOverHttp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/servlet/project-info", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected HTTP 200 OK from HttpServlet");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().contains("E-Commerce Platform - Java Web Project"),
                "Response body must contain project description");
    }

    @Test
    @DisplayName("Direct invocation of HttpServlet doGet() method using mock request and response")
    void testProjectInfoServletDoGetDirectly() throws Exception {
        ProjectInfoServlet servlet = new ProjectInfoServlet();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/servlet/project-info");
        MockHttpServletResponse response = new MockHttpServletResponse();

        servlet.service(request, response);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("E-Commerce Platform - Java Web Project"));
    }
}
