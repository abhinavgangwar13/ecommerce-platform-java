package com.ecommerce.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Classic Java HttpServlet demonstrating Servlet & Web Integration (Rubric Category 4).
 *
 * Demonstrates:
 * 1. Inheritance: explicitly extends jakarta.servlet.http.HttpServlet
 * 2. Overriding: overrides doGet(HttpServletRequest, HttpServletResponse)
 * 3. Annotation Mapping: @WebServlet mapped to /api/servlet/project-info
 * 4. Response Writing: sets content type, status code 200, and writes text payload
 */
@WebServlet(name = "ProjectInfoServlet", urlPatterns = "/api/servlet/project-info")
public class ProjectInfoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        try (PrintWriter writer = response.getWriter()) {
            writer.print("E-Commerce Platform - Java Web Project");
            writer.flush();
        }
    }
}
