package com.ecommerce.config;

import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Configuration for classic Servlet integration.
 * - Enables @ServletComponentScan to detect @WebServlet annotations in com.ecommerce.servlet
 * - Configures WebSecurityCustomizer to allow public access to /api/servlet/** without modifying existing SecurityConfig
 */
@Configuration
@ServletComponentScan(basePackages = "com.ecommerce.servlet")
public class ServletConfig {

    @Bean
    public WebSecurityCustomizer servletWebSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/api/servlet/**");
    }
}
