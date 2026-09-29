# Bookstore API Backend Implementation Plan

## Top-Level Overview
Implementation of the IBM AI Specialist Capstone bookstore backend application using **Java 21, Spring Boot 4.1.1 (Spring 7), PostgreSQL, Spring Data JPA, Bean Validation, Flyway, and Spring Security with JWT**.
The system satisfies the 12 customer journey touchpoints outlined in the capstone specifications, provides an initial OpenAPI 3.1 contract specification as the primary deliverable before code generation, simulates payment workflows, and adheres strictly to the backend coding standards and conventions.

---

## 1. Architecture Feature Scope & Decision Matrix

Review of the Capstone architecture diagram, wireframe flows, and sample prompts vs. the focused backend demonstration scope:

| Architecture Domain / Feature | Status in Demo | Handling & Technical Strategy |
|---|---|---|
| **User Authentication & Profiles** | **Included** | JWT-based stateless authentication (`/api/v1/auth/**`), Customer registration, Current user profile & address management (`/api/v1/users/me/**`). |
| **Catalog, Categories & Brands (Publishers)** | **Included** | Full catalog browsing, pagination, filtering by category/author/publisher, keyword search, tentative delivery date estimate on book details. |
| **Shopping Cart Management** | **Included** | Persistent cart per authenticated customer with stock validation, item quantity updates, and cart item removal. |
| **Simulated Payment Processing** | **Included** | Internal mock payment gateway (`POST /api/v1/orders/{orderId}/payments`) supporting multiple card/payment types, test card outcome triggers, order state transition to `PAID`, stock decrement, and cart clearing. |
| **Gift Points Redemption** | **Included** | Configurable demo conversion rate (`100 points = $1.00`, configurable via `application.properties` as `bookstore.gift-points.rate`), applied at checkout to reduce order total. |
| **Order History, Buy Again & Recommendations** | **Included** | Customer order history, `/orders/buy-again` (distinct past-purchased books), `/recommendations/order-history` (category-based recommendations from user purchase history). |
| **48-Hour Order Cancellation Rule** | **Included** | Strict timestamp comparison (`order.orderedAt.plusHours(48).isAfter(Instant.now())`). Order status transitioned to `CANCELLED` and book inventory restored. |
| **Store Administration (Back-office CRUD)** | **Deferred** | Admin endpoints for adding/editing books/publishers are out of scope for the customer demo. Initial catalog is pre-seeded via Flyway migrations (`V4__seed_initial_data.sql`). |
| **Coupon Engine & Promo Codes** | **Deferred** | Promotional coupon codes validation and stacking logic are deferred. Discount functionality is demonstrated via gift points redemption. |
| **Dynamic Shipping Calculation & Carrier Integration** | **Deferred** | External carrier APIs (FedEx, UPS, DHL) and dynamic distance-based pricing are deferred. Fixed flat-rate shipping ($5.00 or free above threshold) is calculated statically at checkout. |
| **Returns & Refunds Workflow** | **Deferred** | Post-delivery RMA requests, refund ledger adjustments, and courier reverse pickups are deferred. Post-purchase lifecycle is demonstrated via the 48-Hour cancellation window. |
| **Wishlist & Customer Book Reviews** | **Deferred** | Social wishlist and rating/review comments are deferred to maintain focus on the core purchase funnel. |

---

## 2. Customer Journey & Required Backend Behavior

| Touchpoint / Step | Journey Action | Required Backend Behavior & APIs |
|---|---|---|
| **Step 1 & 2** | User Login & Authentication | Register user (`ROLE_CUSTOMER`), authenticate via username/password, issue stateless **JWT token**, return user profile. Support guest browsing for catalog without token. |
| **Step 3, 5, 6** | Browse Catalog & Categories / Brands (Publishers) | `GET /api/v1/categories`, `GET /api/v1/authors`, `GET /api/v1/publishers`, & `GET /api/v1/books` with pagination, category filter (`categoryId`), author filter, publisher/brand filter, and keyword search. |
| **Step 7** | Select Product & Related Products | `GET /api/v1/books/{id}` (includes tentative delivery estimation: `expectedDeliveryDays`, stock status), `GET /api/v1/books/{id}/related` (books in the same category/author). |
| **Step 8** | Shopping Cart Operations | `GET /api/v1/cart`, `POST /api/v1/cart/items` (add book, check stock), `PUT /api/v1/cart/items/{itemId}` (update quantity), `DELETE /api/v1/cart/items/{itemId}` (remove), `DELETE /api/v1/cart` (clear). |
| **Step 4 (Slide 3)** | Recommendations based on Order History / Buy Again | `GET /api/v1/recommendations/order-history` (books based on previous orders or top sellers) and `GET /api/v1/orders/buy-again` (distinct books previously ordered). |
| **Step 9** | Delivery Address Selection | Customer address management: `GET /api/v1/users/me/addresses`, `POST /api/v1/users/me/addresses` or providing shipping address directly in checkout request. |
| **Step 10** | Gift Points & Payment Initiation | `POST /api/v1/orders/checkout` with optional `giftPointsToRedeem` discount, creating order in `PENDING_PAYMENT` state with calculated subtotal, discount, tax, shipping, and total. |
| **Step 11** | Simulated Payment & Confirmation | `POST /api/v1/orders/{orderId}/payments` with simulated payment options (`CREDIT_CARD`, `DEBIT_CARD`, `NET_BANKING`, `UPI`), idempotency, state transition from `PENDING_PAYMENT` -> `PAID`, inventory decrement, and earning reward points. |
| **Step 12** | Purchase Confirmation & 48-Hour Order Cancellation | `GET /api/v1/orders/{orderId}` (view confirmation and status). `POST /api/v1/orders/{orderId}/cancel`: Business rule enforces order cancellation allowed **only within 48 hours** from order placement timestamp, restoring inventory. |

---

## 3. Assumptions & Demo Conventions

1. **OpenAPI-First Delivery**: An OpenAPI 3.1 specification (`docs/openapi.yaml`) is authored and validated before any Java application code is implemented.
2. **Authentication Method**: Stateless JWT Bearer Token (`Authorization: Bearer <token>`). Tokens expire after a configured duration (e.g., 24 hours).
3. **Gift Points Valuation**: Default configuration is `100 points = $1.00 USD` (or 1 point = $0.01). Configured in `application.properties` via `bookstore.gift-points.rate=100`. Max points redeemable cannot exceed the subtotal value.
4. **48-Hour Cancellation Boundary**: Exact timestamp check:
   ```java
   Instant expiryTime = order.getOrderedAt().plus(48, ChronoUnit.HOURS);
   if (clock.instant().isAfter(expiryTime)) {
       throw new OrderCancellationExpiredException("Orders cannot be cancelled after 48 hours from placement time.");
   }
   ```
5. **Simulated Payment Gateway Rules**:
   - `testCardNumber` ending in `0000` -> Fails simulation with `DECLINED_INSUFFICIENT_FUNDS`.
   - Any other card or valid simulated method -> Success, generates transaction reference, transitions order to `PAID`, deducts stock.

---

## 4. Entities, Relationships & Business Rules

```
+---------------+        1..*        +---------------+
|   Category    |<-------------------|     Book      |
+---------------+                    +---------------+
                                        |         |
+---------------+        1..*           |         | 1..*
|    Author     |<----------------------+         +--------------------+
+---------------+                                                      |
                                                                       |
+---------------+        1..*                                          |
|   Publisher   |<-----------------------------------------------------+
+---------------+

+---------------+        1..*        +---------------+
|     User      |<-------------------|  UserAddress  |
+---------------+                    +---------------+
     |       |
1..1 |       | 1..*
     v       v
+--------+ +---------------+         1..*        +---------------+
|  Cart  | |     Order     |-------------------->|   OrderItem   |
+--------+ +---------------+                     +---------------+
     |              |
1..* |         1..1 |
     v              v
+--------+ +---------------+
|CartItem| |    Payment    |
+--------+ +---------------+
```

### Core Entities & Attributes:
1. **User**: `id (UUID)`, `username`, `email`, `passwordHash`, `role (CUSTOMER, ADMIN)`, `rewardPoints`, `active`, `createdAt`, `updatedAt`.
2. **UserAddress**: `id (UUID)`, `user (FK)`, `recipientName`, `phone`, `street`, `city`, `state`, `postalCode`, `country`, `isDefault`.
3. **Category**: `id (UUID)`, `name`, `slug`, `description`.
4. **Author**: `id (UUID)`, `name`, `bio`.
5. **Publisher**: `id (UUID)`, `name`, `website`.
6. **Book**: `id (UUID)`, `title`, `isbn`, `category (FK)`, `author (FK)`, `publisher (FK)`, `description`, `price`, `stockQuantity`, `coverImageUrl`, `expectedDeliveryDays`, `active`, `version` (optimistic lock).
7. **Cart & CartItem**: `Cart` (belongs to `User`), `CartItem` (`cart (FK)`, `book (FK)`, `quantity`, `unitPrice`).
8. **Order & OrderItem**: `Order` (`id (UUID)`, `orderNumber`, `user (FK)`, `shippingAddress (JSON or FK)`, `subtotal`, `discountAmount`, `taxAmount`, `shippingAmount`, `totalAmount`, `pointsRedeemed`, `pointsEarned`, `status`, `orderedAt`, audit fields). `OrderItem` (`order (FK)`, `book (FK)`, `bookTitle`, `quantity`, `unitPrice`, `subtotal`).
9. **Payment**: `id (UUID)`, `order (FK)`, `paymentReference`, `paymentMethod (CREDIT_CARD, DEBIT_CARD, UPI, NET_BANKING)`, `amount`, `status (SUCCESS, FAILED)`, `failureReason`, `paidAt`.

---

## 5. REST API Endpoints Specification

### Authentication & Users
- `POST /api/v1/auth/register` - Register a new customer
- `POST /api/v1/auth/login` - Authenticate and obtain JWT token
- `GET /api/v1/users/me` - Get current authenticated user profile & points
- `GET /api/v1/users/me/addresses` - List customer addresses
- `POST /api/v1/users/me/addresses` - Save a new customer address

### Catalog & Discovery
- `GET /api/v1/categories` - List all book categories
- `GET /api/v1/authors` - List/search authors
- `GET /api/v1/publishers` - List/search publishers (brands)
- `GET /api/v1/books` - Search & filter books (params: `categoryId`, `authorId`, `publisherId`, `query`, `page`, `size`, `sort`)
- `GET /api/v1/books/{id}` - Book details with delivery estimate
- `GET /api/v1/books/{id}/related` - Related books in same category/author

### Shopping Cart
- `GET /api/v1/cart` - View current customer's cart
- `POST /api/v1/cart/items` - Add item to cart `{ "bookId": "...", "quantity": 1 }`
- `PUT /api/v1/cart/items/{itemId}` - Update item quantity
- `DELETE /api/v1/cart/items/{itemId}` - Remove item from cart
- `DELETE /api/v1/cart` - Clear entire cart

### Checkout & Orders
- `POST /api/v1/orders/checkout` - Create order from cart with address and gift points redemption
- `GET /api/v1/orders` - List current customer's order history
- `GET /api/v1/orders/{orderId}` - View specific order details & status
- `POST /api/v1/orders/{orderId}/cancel` - Cancel order within 48 hours
- `GET /api/v1/orders/buy-again` - Quick reorder item list from past orders
- `GET /api/v1/recommendations/order-history` - Recommendations based on past order history

### Payments (Simulated)
- `POST /api/v1/orders/{orderId}/payments` - Process simulated payment (`CREDIT_CARD`, `DEBIT_CARD`, `UPI`, `NET_BANKING`)

---

## 6. Simulated Payment Workflow

```
Customer                    Backend Service                    Database
   |                               |                              |
   |--- POST /orders/checkout ---->|                              |
   |                               |-- Create Order (PENDING) --->|
   |<-- Order Created (orderId) ---|                              |
   |                               |                              |
   |--- POST /orders/{id}/payments>|                              |
   |    {method, cardNumber...}    |-- Validate Order State       |
   |                               |-- Simulate Gateway Checks    |
   |                               |   (Card format, test flags)  |
   |                               |                              |
   |                               |-- If SUCCESS:                |
   |                               |   Order status -> PAID       |
   |                               |   Decrement Book Stocks      |
   |                               |   Credit Reward Points       |
   |                               |   Clear User Cart            |
   |                               |   Save Payment record ------>|
   |<-- Payment Success (Receipt)--|                              |
```

---

## 7. Technical Architecture, Security & Database Migrations

### Architecture Stack & Conventions
- **Language & Runtime**: Java 21
- **Framework**: Spring Boot 4.1.1 (Spring 7 MVC)
- **Database**: PostgreSQL with Flyway migration management
- **Security**: Spring Security filter chain with JJWT (Java JWT) stateless token authentication.
- **Code Style Alignment**:
  - `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)` + `@RequiredArgsConstructor`
  - Constructor injection, no `@Autowired`
  - DTO request/response isolation, MapStruct mappers
  - Thin controllers delegating directly to services
  - Singular package names (`com.ibm.bookstore.controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `exception`, `config`, `security`)

### Database Migrations (`src/main/resources/db/migration`)
- `V1__init_user_schema.sql` - users, user_addresses
- `V2__init_catalog_schema.sql` - categories, authors, publishers, books
- `V3__init_cart_and_order_schema.sql` - carts, cart_items, orders, order_items, payments
- `V4__seed_initial_data.sql` - categories, authors, publishers, sample books, test user

---

## 8. Implementation Sub-Tasks

### Sub-Task 1: OpenAPI 3.1 Specification Deliverable [x]
- **Intent**: Create and save the complete, valid OpenAPI 3.1 YAML specification matching all bookstore endpoints and schemas before writing any Spring Boot application code.
- **Expected Outcomes**: Valid `docs/openapi.yaml` file containing all paths, schemas, JWT security schemes, and status codes.
- **Todo List**:
  1. [x] Author `docs/openapi.yaml` with components for Auth, Catalog, Cart, Order, Recommendation, and Payment schemas.
  2. [x] Validate OpenAPI syntax and verify endpoint consistency.

### Sub-Task 2: Project Configuration & Dependencies Setup [x]
- **Intent**: Configure Maven `pom.xml` with Lombok, MapStruct, JJWT, Springdoc OpenAPI UI compatible with Spring Boot 4.1.1 / Java 21, and setup `application.properties`.
- **Expected Outcomes**: Project compiles cleanly with `mvn clean compile`.
- **Todo List**:
  1. [x] Add JJWT, MapStruct, Lombok, and Springdoc OpenAPI dependencies in `pom.xml`.
  2. [x] Configure `application.properties` with PostgreSQL datasource, Flyway, JWT secret outside hardcoding, and gift points conversion rate (`bookstore.gift-points.rate`).

### Sub-Task 3: Flyway Migrations & Seed Data [x]
- **Intent**: Create PostgreSQL relational schema and initial seed data for local testing.
- **Expected Outcomes**: Database migration scripts run and populate catalog and demo users.
- **Todo List**:
  1. [x] Write `V1__init_user_schema.sql`.
  2. [x] Write `V2__init_catalog_schema.sql`.
  3. [x] Write `V3__init_cart_and_order_schema.sql`.
  4. [x] Write `V4__seed_initial_data.sql`.
  5. [x] Verify OpenAPI contract consistency and schema compatibility.

### Sub-Task 4: Domain Entities & Repositories [x]
- **Intent**: Create JPA entities and Spring Data JPA repositories with proper audit and optimistic locking.
- **Expected Outcomes**: All database entities mapped with `@FieldDefaults` and repository interfaces declared.
- **Todo List**:
  1. [x] Implement `User`, `UserAddress`, `Category`, `Author`, `Publisher`, `Book`.
  2. [x] Implement `Cart`, `CartItem`, `Order`, `OrderItem`, `Payment`.
  3. [x] Create repositories extending `JpaRepository` and `JpaSpecificationExecutor`.
  4. [x] Implement unit tests for domain entities and enum mappings.

### Sub-Task 5: Exception Handling & DTO Mappers [x]
- **Intent**: Provide unified error handling using RFC 7807 `ProblemDetail` and MapStruct mappers.
- **Expected Outcomes**: Custom business exceptions mapped to HTTP status codes with clean DTO conversions.
- **Todo List**:
  1. [x] Implement `GlobalExceptionHandler` with `@RestControllerAdvice`.
  2. [x] Create request/response DTOs corresponding to the OpenAPI specification (`AddressDto`, `CreateAddressRequest`, `AddCartItemRequest`, `UpdateCartItemRequest`, `CartItemResponse`, `CartResponse`, `CheckoutRequest`, `OrderItemResponse`, `OrderSummaryDto`, `OrderResponse`, `PaymentRequest`, `PaymentResponse`, Auth & Catalog DTOs).
  3. [x] MapStruct / Domain mapping: Evaluated and verified. Direct canonical constructors and focused mapping helper methods are used cleanly across Java 21 records and domain entities without unnecessary overhead.

### Sub-Task 6: JWT Security & Authentication Services [x]
- **Intent**: Configure Spring Security filter chain with stateless JWT token validation.
- **Expected Outcomes**: Secure endpoint access with customer registration, login, profile, and address management.
- **Todo List**:
  1. [x] Implement `JwtUtils`, `JwtAuthenticationFilter`, `SecurityConfig`, and password encoder bean.
  2. [x] Implement `AuthService`, `UserService`, `AuthController`, and `UserController` (including address management).

### Sub-Task 7: Catalog & Recommendation Services & Controllers [x]
- **Intent**: Implement book browsing, searching, filtering, detail retrieval, related items, and history recommendations.
- **Expected Outcomes**: Working endpoints for catalog browsing, filters, and recommendations.
- **Todo List**:
  1. [x] Implement category, author, and publisher discovery endpoints in `CatalogService` & `CatalogController`.
  2. [x] Implement book catalog search & filtering (with JPA specification filtering, pagination, and sorting) & related books retrieval in `CatalogService` & `CatalogController`.
  3. [x] Implement `RecommendationService` & `RecommendationController` (order history based recommendation fallback to latest catalog).

### Sub-Task 8: Shopping Cart Service & Controller [x]
- **Intent**: Implement cart management with stock validation.
- **Expected Outcomes**: Customers can add, update, remove items, and view total calculations.
- **Todo List**:
  1. [x] Implement `CartService` with stock validation logic.
  2. [x] Implement `CartController`.

### Sub-Task 9: Order, Checkout & 48-Hour Cancellation Service [x]
- **Intent**: Implement checkout workflow, configurable gift points calculation, order lifecycle, and exact timestamp 48-hour cancellation rule.
- **Expected Outcomes**: Order placement, history lookup, buy-again listing, and enforced 48-hour cancellation.
- **Todo List**:
  1. [x] Implement `OrderService` with points redemption and exact timestamp 48-hour cancellation check.
  2. [x] Implement `OrderController`.

### Sub-Task 10: Simulated Payment Service & Controller
- **Intent**: Implement payment processing simulation with order state updates.
- **Expected Outcomes**: Payment completion transitions orders to `PAID`, decrements stock, and updates reward points.
- **Todo List**:
  1. [x] Implement `PaymentService` (simulation logic, stock decrement, reward points update).
  2. [x] Implement `PaymentController`.

### Sub-Task 11: Validation & Unit/Integration Tests
- **Intent**: Verify system behavior with JUnit 5 tests.
- **Expected Outcomes**: Key business flows (checkout, simulated payment, 48-hour cancellation) verified with passing tests.
- **Todo List**:
  1. Write unit tests for `OrderService` (including exact 48h cancellation timestamp boundary tests).
  2. Write unit tests for `CartService` and `PaymentService`.
  3. Write controller slice tests for catalog and checkout APIs.

---

## 9. Demonstration Checklist

- [ ] **1. OpenAPI Contract**: Validate `docs/openapi.yaml` against OpenAPI 3.1 tools.
- [ ] **2. Application Boot**: Start application via `./mvnw spring-boot:run` and verify Flyway schema execution.
- [ ] **3. Swagger UI / OpenAPI**: Access `http://localhost:8080/swagger-ui.html` to explore interactive API documentation.
- [ ] **4. Catalog & Discovery**: `GET /api/v1/books` (filter by category, author, keyword), `GET /api/v1/books/{id}` (verify delivery days).
- [ ] **5. User Authentication (JWT)**: Register user `POST /api/v1/auth/register`, login `POST /api/v1/auth/login` to obtain JWT Bearer token.
- [ ] **6. Cart Management**: `POST /api/v1/cart/items` (add books), verify cart subtotal.
- [ ] **7. Checkout & Gift Points**: `POST /api/v1/orders/checkout` (apply gift points discount, verify order created in `PENDING_PAYMENT`).
- [ ] **8. Payment Simulation**: `POST /api/v1/orders/{orderId}/payments` (verify status -> `PAID`, stock decremented, reward points earned).
- [ ] **9. 48-Hour Cancellation**: Test `POST /api/v1/orders/{orderId}/cancel` on a new order (success) and simulated expired order (rejection with 400).
- [ ] **10. Recommendations & Buy Again**: `GET /api/v1/orders/buy-again` and `GET /api/v1/recommendations/order-history`.
