# Bookstore API - Local Development Setup & Documentation

Welcome to the Bookstore API, a backend application developed as the capstone project for the **IBM Applied AI Specialist** professional certificate. This repository implements an e-commerce REST API that handles catalogue browsing, shopping carts, order creation, customer profiles, reward-points, recommendations, and simulated payment integrations.

---

## Table of Contents
1. [Project Overview and Features](#project-overview-and-features)
2. [Technology Stack](#technology-stack)
3. [Local Setup](#local-setup)
4. [Build, Test, and Run](#build-test-and-run)
5. [API Usage](#api-usage)
6. [Verification and Documentation](#verification-and-documentation)
7. [AI-Assisted Workflow](#ai-assisted-workflow)
8. [E-Commerce Demo Business Assumptions](#e-commerce-demo-business-assumptions)

---

## Project Overview and Features

The Bookstore API is designed around core e-commerce requirements, supporting both guest and authenticated customer user flows.

### Core Features
*   **Authentication & User Profiles:** Token-based authentication using Spring Security and JSON Web Tokens (JWT). Customers can register, log in, retrieve their profile details, and manage saved shipping addresses.
*   **Catalog Browsing & Filtering:** Supports book search, pagination, sorting, and filtering. Catalog filtering is built using JPA Specifications (`BookSpecification`) enabling query criteria to be combined dynamically.
*   **Shopping Cart Management:** Basic shopping cart operations (add, update item quantity, remove, and clear) with real-time stock-availability checks.
*   **Checkout & Order Placement:** Converts cart items into a pending order. It computes the order subtotal, applies optional reward-points discounts, adds shipping fees, and records order details.
*   **Simulated Payment Gateways:** Simulates real-time payment processing over Credit Card, Net Banking, and UPI methods, allowing testing of different transaction outcomes.
*   **Order Cancellation Window:** Allows users to cancel pending or paid orders within a strict 48-hour window. Cancelling a paid order automatically restores book stock, refunds redeemed reward points, and reverses earned points.
*   **E-Commerce Rewards Program:** Tracks customer reward point balances and logs points earned/redeemed per order. Customers earn points based on their subtotal expenditures and can redeem them for monetary discounts on future purchases.
*   **Personalized Recommendation Engines:**
    *   **Buy-Again Recommendations:** Recommends books previously purchased in completed, PAID, non-cancelled orders.
    *   **Order-History Recommendations:** Recommends books in categories matching the customer's purchase history.
    *   **Automatic Fallback:** Falls back to recommending the latest active catalog books if the customer has no order history.

---

## Technology Stack

The application's backend architecture is built upon the following technologies and verified versions from the `pom.xml`:
*   **Java:** Version `21` (LTS)
*   **Spring Boot:** Version `4.1.1` (Starter parent)
*   **Spring Security & JJWT:** Stateless authentication using `io.jsonwebtoken` JJWT version `0.12.6`.
*   **Spring Data JPA & Hibernate:** Object-Relational Mapping (ORM) and data persistence.
*   **PostgreSQL:** Relational database storage.
*   **Flyway Database Migrations:** Database schema control and demo seeding.
*   **Swagger UI & OpenAPI Integration:** Runtime documentation generated automatically via `springdoc-openapi` version `3.0.0` at `/v3/api-docs` and displayed interactively via `/swagger-ui.html`.
*   **MapStruct & Lombok:** MapStruct `1.6.3` is configured as a dependency and annotation processor. Lombok `1.18.36` (with annotation-processor binding `0.2.0`) is used for code generation.

> ⚠️ **API Contract Distinction:** The project maintains a static, design-first OpenAPI 3.1.0 contract file located at [`docs/openapi.yaml`](docs/openapi.yaml), which serves as the source of truth for the API specifications. This is distinct from the runtime-generated interactive documentation exposed via the Spring Boot server (Swagger UI at `/swagger-ui.html` and raw JSON at `/v3/api-docs`). Actual DTO mapping is handled using explicit, manual mapping code converting entities to Java record types.

---

## Local Setup

### Prerequisites
*   **Java 21 (LTS)** Development Kit (JDK) installed.
*   **PostgreSQL** database server running locally.
*   Maven is provided via the included wrapper (`./mvnw`).

### 1. Database Initialization
Create a PostgreSQL database named `bookstoredb` using your preferred PostgreSQL client or command-line interface:
```sql
CREATE DATABASE bookstoredb;
```

### 2. Environment Variables Configuration
Configure the following environment variables prior to running the application. Replace the placeholders with your actual local database credentials:
```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/bookstoredb"
export SPRING_DATASOURCE_USERNAME="your_username"
export SPRING_DATASOURCE_PASSWORD="your_password"
```

### 3. JWT Secret Configuration
Stateless JWT authentication requires a signing key. Configure the `JWT_SECRET` environment variable with a plain or hex string containing at least 32 bytes (256 bits):
```bash
export JWT_SECRET="$(openssl rand -hex 32)"
```
*Note on Secret Handling:* The application implementation reads the raw UTF-8 bytes of this environment variable directly to initialize the HMAC-SHA signing key. It does not perform Base64 decoding on the value, meaning any plain or hexadecimal string containing sufficient entropy can be used directly.

### 4. Flyway Database Seeding
Upon server startup, Flyway automatically discovers and executes four sequentially ordered SQL migration scripts to build the database schema and populate demo data:
1.  `V1__init_user_schema.sql` (Creates `users` and `user_addresses` tables)
2.  `V2__init_catalog_schema.sql` (Creates `categories`, `authors`, `publishers`, and `books` tables)
3.  `V3__init_cart_and_order_schema.sql` (Creates `carts`, `cart_items`, `orders`, `order_items`, and `payments` tables)
4.  `V4__seed_initial_data.sql` (Seeds reference categories, authors, publishers, and initial books)

---

## Build, Test, and Run

### Build and Package
To build the project and compile class files, run:
```bash
./mvnw clean install
```

### Run Automated Tests
To run the full test suite, execute:
```bash
./mvnw test
```
*Test Strategy & Isolated Database:* The application's test suite consists of both unit tests and integration tests (such as `@SpringBootTest` scenarios like `CustomerJourneyIntegrationTest` and `PaymentLifecycleIntegrationTest`). Because integration tests run against a live PostgreSQL instance and modify demonstration data to verify real-world behavior, it is recommended to use a dedicated local demo or test database rather than an active production database.

*Note on Test Count:* The previously completed build passed all **132 automated tests** successfully.

### Running the Application
Start the Spring Boot application locally:
```bash
./mvnw spring-boot:run
```
By default, the application runs on local port **8080**:
*   **Local API Base URL:** `http://localhost:8080/api/v1`
*   **Interactive Swagger UI:** `http://localhost:8080/swagger-ui.html`
*   **Runtime OpenAPI JSON Docs:** `http://localhost:8080/v3/api-docs`

---

## API Usage

The bookstore backend exposes exactly **23 unique API operations**, organized into 7 core functional areas:

| Area | HTTP Method | Endpoint | Description | Auth Required |
|---|---|---|---|---|
| **Auth** | `POST` | `/api/v1/auth/register` | Register a new customer | No |
| | `POST` | `/api/v1/auth/login` | Log in and receive a JWT token | No |
| **Profile** | `GET` | `/api/v1/users/me` | Retrieve the authenticated user's profile | Yes |
| | `GET` | `/api/v1/users/me/addresses` | List all saved shipping addresses | Yes |
| | `POST` | `/api/v1/users/me/addresses` | Add a new shipping address | Yes |
| **Catalog** | `GET` | `/api/v1/categories` | Retrieve all product categories | No |
| | `GET` | `/api/v1/authors` | Retrieve/search authors | No |
| | `GET` | `/api/v1/publishers` | Retrieve all publishers | No |
| | `GET` | `/api/v1/books` | Page, sort, and filter the book catalog | No |
| | `GET` | `/api/v1/books/{id}` | Retrieve details for a single book | No |
| | `GET` | `/api/v1/books/{id}/related` | Retrieve similar books in the same category | No |
| **Cart** | `GET` | `/api/v1/cart` | Retrieve the active shopping cart | Yes |
| | `POST` | `/api/v1/cart/items` | Add a book to the shopping cart | Yes |
| | `PUT` | `/api/v1/cart/items/{itemId}` | Modify a cart item's quantity | Yes |
| | `DELETE` | `/api/v1/cart/items/{itemId}` | Remove an item from the cart | Yes |
| | `DELETE` | `/api/v1/cart` | Clear the entire shopping cart | Yes |
| **Orders** | `POST` | `/api/v1/orders/checkout` | Convert the cart into a pending order | Yes |
| | `GET` | `/api/v1/orders` | Retrieve authenticated user's order history | Yes |
| | `GET` | `/api/v1/orders/{orderId}` | Retrieve detailed order specifications | Yes |
| | `POST` | `/api/v1/orders/{orderId}/cancel` | Cancel an order if within 48-hour window | Yes |
| | `GET` | `/api/v1/orders/buy-again` | Buy-again recommendations based on PAID, non-cancelled orders | Yes |
| **Payments** | `POST` | `/api/v1/orders/{orderId}/payments` | Submit payment and complete order | Yes |
| **Recommendations** | `GET` | `/api/v1/recommendations/order-history` | Personal recommendations based on order history | Yes |

### Security & JWT Acquisition
To access protected endpoints, a customer must first register or log in. The login/registration response returns a JSON Web Token. For subsequent protected requests, include this token in the `Authorization` header as a Bearer token:
```http
Authorization: Bearer YOUR_TOKEN_HERE
```

### Catalogue Filtering Parameters
The book catalog endpoint (`GET /api/v1/books`) supports the following parameters:
*   `categoryId` (UUID): Filter by category.
*   `authorId` (UUID): Filter by author.
*   `publisherId` (UUID): Filter by publisher.
*   `query` (String): Filter books by title or description keyword.
*   `page` (int): Page index (0-based).
*   `size` (int): Page size.
*   `sort` (String): Sort properties and direction (e.g. `title,asc`).

### Curl Examples (using port 8080)

#### Retrieve Book Catalog (Public Endpoint)
```bash
curl "http://localhost:8080/api/v1/books?page=0&size=5"
```

#### Add Book to Shopping Cart (Protected Endpoint)
```bash
curl -X POST "http://localhost:8080/api/v1/cart/items" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "bookId": "44444444-4444-4444-4444-444444444401",
    "quantity": 1
  }'
```

For the exhaustive request and response schemas, see the design-first API contract file: [`docs/openapi.yaml`](docs/openapi.yaml).

---

## Verification and Documentation

All 23 API operations have been validated through live HTTP request execution on a running server instance.

*   **API Flow Validation:** Every individual operation (including `GET`, `POST`, `PUT`, and `DELETE` requests) was exercised and verified, capturing response headers and payload structures.
*   **Response Integrity:** Seven additional detailed response-schema validation checks were carried out to ensure compliance with OpenAPI contracts.
*   **State Persistence Checks:** Direct SQL database queries and follow-up `GET` endpoints were executed after write operations (`POST`, `PUT`, `DELETE`) to verify that the transactional state was correctly persisted, updated, or reverted in the PostgreSQL backend.

### Documentation & Saved Verification Artifacts
Detailed records and verification evidence are available in the repository:
*   Static Design Contract: [`docs/openapi.yaml`](docs/openapi.yaml)
*   Relational Data Model Markdown: [`docs/data-model.md`](docs/data-model.md)
*   Interactive HTML Data Model: [`docs/data-model.html`](docs/data-model.html) (Can be saved and opened locally in any modern web browser to interactively explore tables, columns, and relationships).
*   Live Verification Report: [`docs/live-api-verification.md`](docs/live-api-verification.md)
*   Raw HTTP Execution Evidence: [`docs/live-api-verification-evidence.txt`](docs/live-api-verification-evidence.txt)
*   Detailed Capstone Implementation Plan: [`bookstore-implementation-plan.md`](bookstore-implementation-plan.md)

> 💡 **Port Context Note:** The saved historical verification logs and implementation plans record a testing environment where port `8081` was used because of a local port conflict. However, the default local configuration and standard setup recommended for developers is standardized on port `8080`.

---

## AI-Assisted Workflow

This capstone application was developed with an AI-assisted workflow.

*   **IBM Bob:** Contributed to the planning, implementation, tests, and documentation of the bookstore application.
*   **IBM Consulting Advantage (ICA):** Separately generated draft test scenarios, which were then reviewed and verified by the developer during test development.

---

## E-Commerce Demo Business Assumptions

The bookstore backend operates under a set of business rules configured in application properties:

### 1. Simulated Payment Gateway
Payment transactions are simulated by backend logic rather than routing to an active external credit card processor or banking portal. Multiple payment methods are supported (`CREDIT_CARD`, `NET_BANKING`, and `UPI`), and the transaction outcomes (such as successful or unsuccessful outcomes) are selected through the `simulationOutcome` request field (e.g. `INSUFFICIENT_FUNDS`, `GATEWAY_TIMEOUT`, and `CARD_EXPIRED`).

### 2. Shipping Logistics
*   **Flat shipping fee:** `$5.00`
*   **Free shipping threshold:** `$50.00`. If the order subtotal is equal to or greater than `$50.00`, shipping is free.

### 3. Reward Points Mechanics
*   **Earning Points:** Successful payment credits points to the user's account at a rate of **1 point per $1.00 spent on subtotal**, floored to the nearest integer (calculated as `floor(order subtotal)`). Points are earned directly on the pre-discount subtotal.
*   **Redemption Rate:** Reward points are redeemed at a conversion rate of **100 points = $1.00** discount (where 1 point = $0.01 discount). The maximum points redeemable on a checkout cannot exceed the subtotal value.
*   **Points Reversal upon Cancellation:** When a PAID order is cancelled within the allowed cancellation window, any points earned from that order are deducted from the customer's balance. If the customer already spent those points on a separate order, the point balance is decremented and floored at `0`. Any points redeemed during checkout are refunded back to the user's account.

### 4. Cancellation Window
Orders are eligible for cancellation for exactly **48 hours** from the instant of placement. After 48 hours, cancellations are blocked, and attempting to cancel throws an `OrderCancellationExpiredException`.

### 5. Personalized Recommendations
Recommendations are computed dynamically using implemented business logic on past purchases. If a customer has no order history, the recommendations system gracefully falls back to displaying the latest active books in the catalog.

### 6. Environment Scope
This project is a local demonstration backend. It does not include any frontend/UI implementation, cloud-based microservices, or production-ready deployment components.
