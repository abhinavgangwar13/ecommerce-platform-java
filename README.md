# Online E-Commerce Platform

A robust, full-stack Java web application built using **Java 21**, **Spring Boot 3.3.4**, **MySQL 8**, **Spring Security (JWT)**, and a responsive frontend using **HTML5, CSS3, JavaScript, and Bootstrap 5.3**. 

Developed as an academic Java Web-Based Project submission, this platform demonstrates core Java programming principles, layered enterprise architecture, explicit JDBC database integration, multi-threading with thread synchronization, and classic Java Servlet integration alongside modern Spring MVC REST controllers.

---

## Table of Contents
- [1. Project Overview](#1-project-overview)
- [2. Problem Statement](#2-problem-statement)
- [3. Objectives](#3-objectives)
- [4. User Roles and Features](#4-user-roles-and-features)
- [5. Technology Stack](#5-technology-stack)
- [6. Architecture](#6-architecture)
- [7. Project Structure](#7-project-structure)
- [8. Database Setup](#8-database-setup)
- [9. Configuration](#9-configuration)
- [10. Prerequisites](#10-prerequisites)
- [11. How to Run](#11-how-to-run)
- [12. Testing & Quality Assurance](#12-testing--quality-assurance)
- [13. Security Implementation](#13-security-implementation)
- [14. Java Concepts Demonstrated](#14-java-concepts-demonstrated)
- [15. Academic Rubric Alignment](#15-academic-rubric-alignment)
- [16. Future Enhancements](#16-future-enhancements)
- [17. Author](#17-author)
- [18. License & Academic Disclaimer](#18-license--academic-disclaimer)

---

## 1. Project Overview
The **Online E-Commerce Platform** is a web-based e-commerce solution engineered to simulate a real-world commercial marketplace. It provides seamless interaction between consumers (buyers), product vendors (sellers), and store managers (administrators). 

The backend is developed with Spring Boot and provides stateless RESTful APIs secured with JSON Web Tokens (JWT) and Role-Based Access Control (RBAC). In addition to Spring Data JPA/Hibernate for object-relational mapping, the project includes explicit, student-written JDBC code to demonstrate low-level database operations, as well as a classic `HttpServlet` implementation and multi-threaded background notification processing.

---

## 2. Problem Statement
Traditional retail systems and monolithic web applications often suffer from tight coupling, insecure authentication, lack of concurrency control during inventory operations, and poor separation of user permissions.

This project addresses these challenges by delivering:
- A secure, role-restricted web platform separating **BUYER**, **SELLER**, and **ADMIN** workflows.
- Real-time catalog searching and multi-attribute filtering.
- Transactional shopping cart management and order placement with inventory deduction and restoration.
- Dedicated dashboards displaying relevant business metrics for each actor.

---

## 3. Objectives
- **Secure Authentication & Authorization:** Implement stateless JWT authentication with BCrypt password hashing and role-based route guards.
- **Product Management:** Enable sellers to create, view, update, and delete catalog items with inventory tracking.
- **Search & Filtering:** Provide buyers with dynamic filtering by category, price range, stock availability, and keyword search with pagination and sorting.
- **Cart & Order Processing:** Manage user carts, validate stock levels, execute atomic order placement, and allow order cancellations with inventory restoration.
- **Role-Based Dashboards:** Deliver tailored metrics and analytics for buyers, sellers, and platform administrators.
- **Dual Database Integration:** Demonstrate enterprise JPA/Hibernate alongside low-level explicit JDBC (`Connection`, `PreparedStatement`, `ResultSet`).
- **Responsive UI:** Offer a clean, mobile-responsive user interface using Vanilla JavaScript and Bootstrap 5.3 with Indian Rupee (₹) formatting.

---

## 4. User Roles and Features

### 👤 Buyer
- **Authentication:** Registration and login with JWT issuance.
- **Product Browsing:** View paginated product catalog with real-time stock status.
- **Search & Filter:** Search by keywords, filter by category, minimum/maximum price, and in-stock status; sort by price or date.
- **Shopping Cart:** Add items, increment/decrement quantities with stock limits, remove items, or clear cart.
- **Checkout & Order Placement:** Review non-editable order items, enter delivery address, and place orders atomically.
- **Order History:** View chronological order history with detailed line-item breakdown (price snapshot, quantities, order date, status).
- **Order Cancellation:** Cancel eligible orders in `PLACED` status, automatically restoring deducted inventory.
- **Buyer Dashboard:** Monitor total orders, total items purchased, total expenditure (₹), and review recent orders.

### 🏪 Seller
- **Authentication:** Seller registration and login.
- **Product Catalog Management:** Create new products, edit descriptions, adjust pricing, and delete seller-owned products.
- **Inventory Stock Updates:** Update available inventory via dedicated stock management modals.
- **Seller Product View:** View and filter only products belonging to the logged-in seller.
- **Seller Dashboard:** Real-time visibility into total products listed, total stock in inventory, total orders placed, and gross sales revenue (₹).

### 🛡️ Admin
- **Administrative Authentication:** Secure administrative access restricted by role guards.
- **Admin Dashboard:** Platform-wide oversight displaying total registered users (breakdown by buyers/sellers), total catalog items, total units in stock, and total system orders.
- **Recent Platform Activity:** View recently registered users and recently placed orders across all buyers.
- **Explicit JDBC Product Statistics:** Access a dedicated endpoint (`GET /api/admin/jdbc/product-stats`) demonstrating direct JDBC querying for inventory metrics.

---

## 5. Technology Stack

| Layer | Technologies & Tools |
| :--- | :--- |
| **Language** | Java 21 (LTS) |
| **Framework** | Spring Boot 3.3.4 (Spring MVC, Spring Security) |
| **Build & Dependency Tool** | Apache Maven 3.9.9 (Maven Wrapper included) |
| **Security & Auth** | Spring Security 6, JJWT 0.12.6, BCrypt Password Hashing |
| **Database & ORM** | MySQL 8.0+, Spring Data JPA, Hibernate, Explicit JDBC |
| **Web & Servlets** | Jakarta Servlet API (`jakarta.servlet.http.HttpServlet`) |
| **Concurrency** | Java Concurrency API (`Runnable`, `Thread`, `ExecutorService`, `synchronized`) |
| **Frontend** | HTML5, CSS3, JavaScript (ES6+ Fetch API), Bootstrap 5.3, Bootstrap Icons |
| **Testing** | JUnit 5 (Jupiter), Spring Boot Test, MockMvc, TestRestTemplate |

---

## 6. Architecture

The application adopts a **Layered Enterprise Architecture** with clear separation of concerns:

```
[ Web Browser / Client UI ]
            │ (HTTP / JSON / Fetch API)
            ▼
[ Security Filter Chain: JwtAuthenticationFilter ]
            │
            ├──────────────────────────┬──────────────────────────┐
            ▼                          ▼                          ▼
 [ Spring MVC REST Controllers ] [ Explicit JDBC Controller ] [ Classic HttpServlet ]
            │                          │                          │
            ▼                          ▼                          ▼
   [ Service Layer ]       [ ProductStatsJdbcDao ]      [ ProjectInfoServlet ]
   (OrderService, etc.)                │
            │                          │ (java.sql.Connection)
            ▼                          ▼
 [ Spring Data JPA Repositories ] ─────┴───────────────► [ MySQL Database ]
            │                                             (ecommerce_db)
            ▼
[ Async Thread Worker Pool: OrderNotificationService ]
```

### Key Architectural Highlights:
1. **Spring MVC REST API:** RESTful controllers handle client requests, validate payloads with Jakarta Validation, and return structured JSON responses.
2. **Spring Data JPA & Hibernate:** Primary object-relational mapping mechanism managing entity relationships (`User`, `Product`, `Cart`, `CartItem`, `Order`, `OrderItem`).
3. **Explicit JDBC DAO (`ProductStatsJdbcDao`):** Demonstrates manual resource management using `java.sql.Connection`, `java.sql.PreparedStatement` with parameterized queries (`?`), `java.sql.ResultSet`, and `try-with-resources`.
4. **Classic Servlet Integration (`ProjectInfoServlet`):** Extends `jakarta.servlet.http.HttpServlet`, demonstrating `doGet(HttpServletRequest, HttpServletResponse)` registered via `@WebServlet` and `@ServletComponentScan`.
5. **Multithreaded Background Notification (`OrderNotificationService`):** Background order notification dispatching using `java.lang.Runnable` tasks executed on a managed `ExecutorService` thread pool, with shared in-memory audit state protected by explicit `synchronized` blocks.

---

## 7. Project Structure

```
ecommerce-platform/
├── .mvn/wrapper/                  # Maven Wrapper configuration and jar
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/
│   │   │   ├── config/            # SecurityConfig, ServletConfig, CORS setup
│   │   │   ├── controller/        # Auth, Product, Cart, Order, Dashboard, JDBC controllers
│   │   │   ├── dao/               # Explicit JDBC DAO (ProductStatsJdbcDao)
│   │   │   ├── dto/               # Request and Response Data Transfer Objects
│   │   │   ├── entity/            # JPA Entities (User, Product, Cart, Order, etc.)
│   │   │   ├── enums/             # Enums (Role, OrderStatus)
│   │   │   ├── exception/         # GlobalExceptionHandler and custom exceptions
│   │   │   ├── repository/        # Spring Data JPA Repositories & Specifications
│   │   │   ├── security/          # JwtTokenProvider, JwtAuthenticationFilter, UserDetails
│   │   │   ├── service/           # Business logic services (Auth, Product, Order, Cart)
│   │   │   │   └── async/         # Multi-threaded background tasks (OrderNotificationService)
│   │   │   ├── servlet/           # Classic HttpServlet implementation (ProjectInfoServlet)
│   │   │   └── EcommerceApplication.java # Spring Boot entry point
│   │   └── resources/
│   │       ├── static/            # Frontend pages (index, login, register, cart, orders, etc.)
│   │       │   ├── css/           # style.css
│   │       │   └── js/            # api.js, auth.js
│   │       ├── application.properties         # Environment-driven configuration
│   │       └── application.properties.example # Safe configuration template
│   └── test/
│       └── java/com/ecommerce/    # 82 automated integration & unit tests
├── .gitignore                     # Git ignore rules for build artifacts and secrets
├── mvnw                           # Linux/macOS Maven wrapper script
├── mvnw.cmd                       # Windows Maven wrapper script
├── pom.xml                        # Project dependencies and Maven build configuration
└── README.md                      # Project documentation
```

---

## 8. Database Setup

The application connects to a dedicated MySQL database named **`ecommerce_db`**.

### Database Creation Script
Run the following SQL statement in your MySQL client (MySQL Workbench, Command Line, or phpMyAdmin):

```sql
CREATE DATABASE ecommerce_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> **Note on Database Isolation:**
> The application operates strictly within `ecommerce_db`. It does not connect to, query, or modify any other database (such as `companydb` or system schemas). All tables and relationships are automatically generated and updated by Hibernate using `spring.jpa.hibernate.ddl-auto=update`.

---

## 9. Configuration

To safeguard sensitive credentials for public version control, the application externalizes database passwords and JWT signing keys using environment variables.

### Safe Environment Variable Template
Refer to [`src/main/resources/application.properties.example`](src/main/resources/application.properties.example) for configuration keys:

```properties
# Database Configuration
DB_USERNAME=root
DB_PASSWORD=your_database_password

# JWT Signing Secret (Must be at least 256 bits / 32 characters for HMAC-SHA256)
JWT_SECRET=your_secure_256_bit_jwt_secret_key_here
```

### Setting Environment Variables in Windows (PowerShell)
Before running the application, set your local credentials in your terminal session:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_database_password"
$env:JWT_SECRET="YourSecure256BitSecretKeyForJwtSigningHere2026"
```

---

## 10. Prerequisites

Before running the application, ensure your environment has:
1. **Java Development Kit (JDK) 21** or higher (`java -version`).
2. **MySQL Server 8.0+** running locally on port `3306`.
3. **Git** for version control.
4. Internet access on the initial run to download Maven dependencies via the included Maven Wrapper.

---

## 11. How to Run

Follow these steps to run the application on Windows:

### Step 1: Clone the Repository
```bash
git clone <repository-url>
cd "E-COMMERCE PRJ"
```

### Step 2: Create the Database
Log into MySQL and create the database:
```sql
CREATE DATABASE ecommerce_db;
```

### Step 3: Configure Environment Variables (PowerShell)
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="Your256BitSecretKeyMustBeAtLeast32CharactersLong2026"
```

### Step 4: Run the Application
Start the Spring Boot backend server using the Maven wrapper:
```powershell
.\mvnw.cmd spring-boot:run
```

### Step 5: Access the Platform
Once started (`Started EcommerceApplication in ... seconds`), open your browser and navigate to:
- **Landing Page:** [http://localhost:8080/](http://localhost:8080/)
- **Product Catalog:** [http://localhost:8080/products.html](http://localhost:8080/products.html)
- **Login / Register:** [http://localhost:8080/login.html](http://localhost:8080/login.html)
- **Classic Servlet Endpoint:** [http://localhost:8080/api/servlet/project-info](http://localhost:8080/api/servlet/project-info)

---

## 12. Testing & Quality Assurance

The project includes an automated test suite verifying all layers of the application.

### Test Execution Command
Run the complete test suite with the Maven wrapper:
```powershell
.\mvnw.cmd clean test
```

### Test Results
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 82, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

### Test Coverage Highlights:
- **Authentication & Security:** User registration, password hashing verification, JWT token issuance, validation, tamper-proofing, role authorization boundaries.
- **Product Catalog & Search:** Catalog creation, updates, deletions, keyword search, price range filtering, multi-condition JPA Specification queries.
- **Cart & Order Processing:** Cart addition, quantity adjustments, stock deduction, atomic order placement, order cancellation, and inventory stock restoration.
- **Dashboards:** Metrics calculation for Buyer, Seller, and Admin dashboards.
- **Explicit JDBC:** Parameterized SQL queries, connection acquisition, statement preparation, and result processing.
- **Multithreading & Concurrency:** 50-thread concurrent execution stress test proving thread-safe state mutation and absence of race conditions using synchronization.
- **Classic Servlet Integration:** Live HTTP and mock dispatch tests verifying servlet execution and status code handling.

---

## 13. Security Implementation

- **Password Hashing:** Passwords are encrypted before database storage using BCrypt (`BCryptPasswordEncoder`) with unique per-password salt generation.
- **Stateless JWT Tokens:** Authentication is token-based using digitally signed HMAC-SHA256 JSON Web Tokens. No session state is held on the server.
- **Role-Based Access Control (RBAC):** HTTP endpoints are protected with URL pattern matching and `@PreAuthorize` method security (`BUYER`, `SELLER`, `ADMIN`).
- **Resource Ownership Enforcement:** Buyers can only access and cancel their own orders; sellers can only edit and delete their own products.
- **Credential Protection:** Zero passwords, database credentials, or secret keys are stored in source code. Configuration is managed via environment variables and excluded by `.gitignore`.

---

## 14. Java Concepts Demonstrated

This project showcases core and advanced Java concepts required in enterprise applications:

1. **Object-Oriented Programming (OOP):**
   - **Encapsulation:** Private entity fields accessed via controlled getters and setters; encapsulated domain rules in entities.
   - **Inheritance:** `ProjectInfoServlet extends HttpServlet`, `ResourceNotFoundException extends RuntimeException`, `JwtAuthenticationFilter extends OncePerRequestFilter`.
   - **Polymorphism:** Method overriding (`doGet`, `doFilterInternal`, `loadUserByUsername`), interface implementations (`UserDetailsService`, `Specification`).
2. **Interfaces & Abstractions:** Spring Data JPA repository interfaces, custom DAO interfaces, and service contracts.
3. **Exception Handling:** Robust exception management using custom exceptions and a centralized `@RestControllerAdvice` (`GlobalExceptionHandler`).
4. **Collections Framework & Generics:** Extensive use of `List`, `Set`, `Map`, `ArrayList`, Streams API, and type-safe generic DTOs (`PagedResponse<T>`).
5. **Multithreading & Concurrency:**
   - `java.lang.Runnable` task encapsulation (`OrderNotificationTask`).
   - Direct `java.lang.Thread` instantiation and execution.
   - Thread pool orchestration via `java.util.concurrent.ExecutorService`.
   - Thread synchronization using explicit `synchronized` monitor blocks to protect shared mutable state during concurrent execution.
6. **Database Connectivity (JDBC):** Explicit low-level usage of `Connection`, `PreparedStatement`, `ResultSet`, and `try-with-resources`.
7. **Servlets & Web Integration:** Low-level HTTP request/response handling via `HttpServlet`, request dispatcher, and servlet lifecycle methods.

---

## 15. Academic Rubric Alignment

| Rubric Area | Project Implementation |
| :--- | :--- |
| **1. Problem Understanding & Solution Design** | Complete end-to-end e-commerce domain design featuring three distinct actor roles (Buyer, Seller, Admin), clear entity-relationship modeling, and responsive web integration. |
| **2. Core Java Concepts** | Comprehensive application of OOP, custom exception handling, Collections API, explicit `Runnable` / `Thread` / `ExecutorService` multithreading, and `synchronized` thread safety. |
| **3. Database Integration (JDBC)** | Dual approach combining high-level Spring Data JPA/Hibernate ORM with student-written, low-level JDBC DAO code demonstrating parameterized queries and result processing. |
| **4. Servlets & Web Integration** | Clean coexistence of Spring MVC REST architecture with a classic `HttpServlet` implementation handling HTTP requests and responses. |

---

## 16. Future Enhancements
The following features are conceptual enhancements identified for future iteration:
- **Payment Gateway Integration:** Direct checkout integration with third-party payment providers (Stripe, Razorpay).
- **Cloud Storage:** Image asset uploads to cloud object storage (AWS S3, Cloudinary).
- **OAuth2 Social Sign-In:** Third-party federated login (Google, GitHub).
- **Real-Time Order Notifications:** WebSocket-based live order status push notifications.
- **Automated Email/SMS Dispatch:** Integration with external messaging services (SendGrid, Twilio) for order delivery updates.

---

## 17. Author
- **Abhinav Gangwar**

---

## 18. License & Academic Disclaimer
*This project was developed strictly for academic purposes as part of a college Java Web-Based Project submission.*
