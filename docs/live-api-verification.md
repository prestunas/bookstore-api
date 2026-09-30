# Bookstore API — Live API Verification Report

**IBM AI Specialist Capstone — Step 8: Live API Verification**

| Field | Value |
|---|---|
| Verification Date | 2026-09-30 |
| Base URL | http://localhost:8081 |
| Git Commit | `88e0879` — docs: add data model documentation and HTML diagram |
| OpenAPI Contract | `docs/openapi.yaml` |
| Evidence File | [live-api-verification-evidence.txt](./live-api-verification-evidence.txt) |
| PostgreSQL | localhost:5432 / bookstoredb |

---

## Environment Notes

The primary application instance (PID 96998) was running on `*:8080` but had an
empty `JWT_SECRET`, making all authenticated endpoints non-functional.
An `oc port-forward` process also occupies `localhost:8080`, routing traffic to a
different backend for localhost connections. Per the project README, a new instance
(PID 98988) was started on **port 8081** with a proper `JWT_SECRET` environment
variable, connecting to the same local PostgreSQL database.
No code, migration, or configuration was modified.

---

## OpenAPI Endpoint Inventory

Total unique operations in `docs/openapi.yaml`: **23**

| Method | Count | Paths |
|---|---|---|
| GET | 13 | `/api/v1/authors`, `/api/v1/books`, `/api/v1/books/{id}`, `/api/v1/books/{id}/related`, `/api/v1/cart`, `/api/v1/categories`, `/api/v1/orders`, `/api/v1/orders/buy-again`, `/api/v1/orders/{orderId}`, `/api/v1/publishers`, `/api/v1/recommendations/order-history`, `/api/v1/users/me`, `/api/v1/users/me/addresses` |
| POST | 7 | `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/cart/items`, `/api/v1/orders/checkout`, `/api/v1/orders/{orderId}/cancel`, `/api/v1/orders/{orderId}/payments`, `/api/v1/users/me/addresses` |
| PUT | 1 | `/api/v1/cart/items/{itemId}` |
| DELETE | 2 | `/api/v1/cart`, `/api/v1/cart/items/{itemId}` |

---

## Operation Coverage Matrix

Each row is one unique OpenAPI method/path. Additional error-scenario checks are listed separately below.

| # | Method | Path | Primary Check ID | Schema Check ID | Actual Status | Expected | Result |
|---|---|---|---|---|---|---|---|
| 1 | POST | `/api/v1/auth/register` | CHK-001 | — | 201 | 201 | **PASS** |
| 2 | POST | `/api/v1/auth/login` | CHK-002 | — | 200 | 200 | **PASS** |
| 3 | GET | `/api/v1/users/me` | CHK-005 | — | 200 | 200 | **PASS** |
| 4 | GET | `/api/v1/users/me/addresses` | CHK-015 | — | 200 | 200 | **PASS** |
| 5 | POST | `/api/v1/users/me/addresses` | CHK-014 | — | 201 | 201 | **PASS** |
| 6 | GET | `/api/v1/categories` | CHK-007 | — | 200 | 200 | **PASS** |
| 7 | GET | `/api/v1/authors` | CHK-008 | — | 200 | 200 | **PASS** |
| 8 | GET | `/api/v1/publishers` | CHK-009 | — | 200 | 200 | **PASS** |
| 9 | GET | `/api/v1/books` | CHK-010 | SCH-001 | 200 | 200 | **PASS** |
| 10 | GET | `/api/v1/books/{id}` | CHK-011 | SCH-002 | 200 | 200 | **PASS** |
| 11 | GET | `/api/v1/books/{id}/related` | CHK-012 | SCH-003 | 200 | 200 | **PASS** |
| 12 | GET | `/api/v1/cart` | CHK-017 | SCH-004 | 200 | 200 | **PASS** |
| 13 | DELETE | `/api/v1/cart` | CHK-022 | — | 204 | 204 | **PASS** |
| 14 | POST | `/api/v1/cart/items` | CHK-016 | — | 200 | 200 | **PASS** |
| 15 | PUT | `/api/v1/cart/items/{itemId}` | CHK-018 | SCH-005 | 200 | 200 | **PASS** |
| 16 | DELETE | `/api/v1/cart/items/{itemId}` | CHK-020 | — | 200 | 200 | **PASS** |
| 17 | POST | `/api/v1/orders/checkout` | CHK-023 | — | 201 | 201 | **PASS** |
| 18 | GET | `/api/v1/orders` | CHK-024 | — | 200 | 200 | **PASS** |
| 19 | GET | `/api/v1/orders/{orderId}` | CHK-025 | — | 200 | 200 | **PASS** |
| 20 | POST | `/api/v1/orders/{orderId}/cancel` | CHK-027 | — | 200 | 200 | **PASS** |
| 21 | GET | `/api/v1/orders/buy-again` | CHK-028 | SCH-006 | 200 | 200 | **PASS** |
| 22 | POST | `/api/v1/orders/{orderId}/payments` | CHK-026 | — | 200 | 200 | **PASS** |
| 23 | GET | `/api/v1/recommendations/order-history` | CHK-029 | SCH-007 | 200 | 200 | **PASS** |

### Additional Error / Edge Checks

| Check ID | Scenario | Actual Status | Expected | Result |
|---|---|---|---|---|
| CHK-003 | POST `/api/v1/auth/register` — duplicate username | 409 | 409 | **PASS** |
| CHK-004 | POST `/api/v1/auth/login` — wrong password | 401 | 401 | **PASS** |
| CHK-006 | GET `/api/v1/users/me` — no token | 401 | 401 | **PASS** |
| CHK-013 | GET `/api/v1/books/{id}` — book not found | 404 | 404 | **PASS** |
| CHK-019 | GET `/api/v1/cart` — persistence after PUT | 200 | 200 | **PASS** |
| CHK-021 | GET `/api/v1/cart` — persistence after DELETE item | 200 | 200 | **PASS** |
| CHK-022-verify | GET `/api/v1/cart` — persistence after clear cart | 200 | 200 | **PASS** |
| CHK-030 | POST payments — INSUFFICIENT_FUNDS simulation | 400 | 400 | **PASS** |
| CHK-031 | POST cancel — PENDING_PAYMENT order | 200 | 200 | **PASS** |
| CHK-032 | GET `/api/v1/cart` — no token | 401 | 401 | **PASS** |
| CHK-033 | GET `/api/v1/authors?query=Martin` — filtered | 200 | 200 | **PASS** |
| CHK-034 | GET `/api/v1/books?query=clean` — filtered | 200 | 200 | **PASS** |

---

## Coverage Summary

| Metric | Value |
|---|---|
| Total OpenAPI operations (unique method/path pairs) | 23 |
| Operations verified via live HTTP | 23 |
| Additional error / edge scenario checks | 12 |
| Schema-compliance checks (SCH-001 – SCH-007) | 7 |
| Operation-level failures | 0 |
| Operations NOT VERIFIED | 0 |

**All 23 OpenAPI operations verified. HTTP methods covered: GET, POST, PUT, DELETE.**

---

## Response Schema Compliance

Schema compliance is assessed against the OpenAPI required-field lists.
Complete sanitized response bodies are in the evidence file at the SCH-nnn entries.

### BookPageResponse + BookSummaryDto — SCH-001

`GET /api/v1/books?page=0&size=3` — captured 2026-09-30T06:42:43Z

| Field | Required | Present | Type | Value (sample) | Result |
|---|---|---|---|---|---|
| `content` (array) | yes | yes | array | 3 items | **PASS** |
| `page` | yes | yes | integer | 0 | **PASS** |
| `size` | yes | yes | integer | 3 | **PASS** |
| `totalElements` | yes | yes | integer | 5 | **PASS** |
| `totalPages` | yes | yes | integer | 2 | **PASS** |
| `last` | yes | yes | boolean | false | **PASS** |
| `content[0].id` | yes | yes | string/UUID | `44444444-...-444444444402` | **PASS** |
| `content[0].title` | yes | yes | string | Clean Architecture... | **PASS** |
| `content[0].isbn` | yes | yes | string | 978-0134494166 | **PASS** |
| `content[0].price` | yes | yes | number | 39.5 | **PASS** |
| `content[0].stockQuantity` | yes | yes | integer | 17 | **PASS** |
| `content[0].authorName` | yes | yes | string | Robert C. Martin | **PASS** |
| `content[0].categoryName` | yes | yes | string | Software Engineering | **PASS** |
| `content[0].expectedDeliveryDays` | yes | yes | integer | 3 | **PASS** |
| `content[0].coverImageUrl` | no (optional) | yes | string | https://... | **PASS** |

### BookDetailDto — SCH-002

`GET /api/v1/books/44444444-4444-4444-4444-444444444402` — captured 2026-09-30T06:42:50Z

| Field | Required | Present | Type | Value | Result |
|---|---|---|---|---|---|
| `id` | yes | yes | string/UUID | `44444444-...-444444444402` | **PASS** |
| `title` | yes | yes | string | Clean Architecture... | **PASS** |
| `isbn` | yes | yes | string | 978-0134494166 | **PASS** |
| `price` | yes | yes | number | 39.5 | **PASS** |
| `stockQuantity` | yes | yes | integer | 17 | **PASS** |
| `expectedDeliveryDays` | yes | yes | integer | **3** | **PASS** |
| `author` | yes | yes | object | `{id, name, bio}` | **PASS** |
| `category` | yes | yes | object | `{id, name, slug, description}` | **PASS** |
| `publisher` | yes | yes | object | `{id, name, website}` | **PASS** |
| `description` | no (optional) | yes | string | non-null | **PASS** |
| `coverImageUrl` | no (optional) | yes | string | non-null | **PASS** |

**Resolved:** The prior report stated `estimatedDeliveryDays=null`. This was a wrong
field name. The actual field is `expectedDeliveryDays` (matching the schema) and its
value is `3` (integer, non-null). There is no contract violation.

### BookSummaryDto — SCH-003 (related), SCH-006 (buy-again), SCH-007 (recommendations)

All three endpoints return `BookSummaryDto[]`. Required fields verified in each:
`id`, `title`, `isbn`, `price`, `stockQuantity`, `authorName`, `categoryName`,
`expectedDeliveryDays` — all present, correct types, non-null in every response.
Result: **PASS** (SCH-003, SCH-006, SCH-007)

### CartResponse + CartItemResponse — SCH-004, SCH-005

`GET /api/v1/cart` and `PUT /api/v1/cart/items/{itemId}` — captured 2026-09-30T06:43:xx

| Field | Required | Present | Type | Value | Result |
|---|---|---|---|---|---|
| `CartResponse.id` | yes | yes | string/UUID | `2ee78e42-...` | **PASS** |
| `CartResponse.items` | yes | yes | array | 1 item | **PASS** |
| `CartResponse.totalQuantity` | yes | yes | integer | 1 / 2 | **PASS** |
| `CartResponse.totalAmount` | yes | yes | number | 39.5 / 79.0 | **PASS** |
| `items[0].id` | yes | yes | string/UUID | `fec751dc-...` | **PASS** |
| `items[0].bookId` | yes | yes | string/UUID | `44444444-...-444444444402` | **PASS** |
| `items[0].bookTitle` | yes | yes | string | Clean Architecture... | **PASS** |
| `items[0].bookIsbn` | yes | yes | string | 978-0134494166 | **PASS** |
| `items[0].coverImageUrl` | yes | yes | string | https://... | **PASS** |
| `items[0].unitPrice` | yes | yes | number | 39.5 | **PASS** |
| `items[0].quantity` | yes | yes | integer | 1 / 2 | **PASS** |
| `items[0].subtotal` | yes | yes | number | 39.5 / 79.0 | **PASS** |
| `items[0].expectedDeliveryDays` | yes | yes | integer | 3 | **PASS** |

**Resolved:** The prior report noted uncertainty about a nested `book` object.
The actual response uses flat fields (`bookId`, `bookTitle`, `bookIsbn`, etc.)
exactly as specified in `CartItemResponse`. No nested object; no contract mismatch.

---

## Cart PUT & DELETE Verification

### PUT /api/v1/cart/items/{itemId} — step-by-step

| Step | Action | Expected | Actual | SQL Evidence | Result |
|---|---|---|---|---|---|
| 1 | POST add Clean Architecture qty=1 | item created, id=`fec751dc-...` | confirmed | — | PASS |
| 2 | GET cart | quantity=1, subtotal=39.5 | confirmed | — | PASS |
| 3 | PUT `{"quantity":2}` | HTTP 200, qty=2, subtotal=79.0, totalAmount=79.0 | confirmed | — | PASS |
| 4 | Verify recalculation | subtotal=2×39.5=79.0, totalQuantity=2, totalAmount=79.0 | confirmed | — | PASS |
| 5 | GET cart (persistence) | quantity=2 persisted | confirmed | DB-NEW-001: quantity=2 in cart_items | PASS |
| 6 | DELETE `/api/v1/cart/items/fec751dc-...` | HTTP 200, items=[] | confirmed | — | PASS |
| 7 | Verify removal | items empty, totalAmount=0 | confirmed | — | PASS |
| 8 | GET cart (persistence) | items=[] | confirmed | DB-NEW-002: 0 rows in cart_items | PASS |

### DELETE /api/v1/cart (clear cart)

| Step | Action | Expected | Actual | Result |
|---|---|---|---|---|
| Setup | Added 2 items to cart | — | 2 items present | PASS |
| Clear | DELETE /api/v1/cart | HTTP 204, empty body | 204, empty body | PASS |
| Verify | GET /api/v1/cart | items=[], totalAmount=0 | confirmed | PASS |

---

## Database Persistence Verification

All SQL statements are read-only SELECT queries. Complete queries and captured results
are in the evidence file at the DB-nnn entries.

| Check ID | Table(s) | Description | Key Result | Result |
|---|---|---|---|---|
| DB-001 | `users` | User captest001 created at registration | id, role=ROLE_CUSTOMER, active=true, created_at=2026-09-30 08:57:50 | **PASS** |
| DB-002 | `user_addresses` | Address created for captest001 | id=5818f14f-..., is_default=true | **PASS** |
| DB-003 | `orders` | 3 orders for captest001 | CANCELLED/PAID/CANCELLED with correct amounts | **PASS** |
| DB-004 | `order_items`, `books` | Items for PAID order 2c8616ea | unit_price=39.50, qty=1, subtotal=39.50 | **PASS** |
| DB-005 | `payments` | Payment for PAID order | CREDIT_CARD, amount=44.50, status=SUCCESS | **PASS** |
| DB-006 | `books` | Stock for Clean Architecture after all ops | 17 (seed=18, 1 net PAID purchase) | **PASS** |
| DB-007 | `users` | Reward points for captest001 | 39 (floor($39.50)=39 pts, 1 net PAID order) | **PASS** |
| DB-008 | `cart_items` | Cart state after all operations | 0 rows — cart empty | **PASS** |
| DB-009 | `payments` | All 4 payment records | 2 SUCCESS, 2 FAILED | **PASS** |
| DB-010 | `books` | Final stock for both tested books | Clean Architecture=17, Clean Code=23 | **PASS** |
| DB-NEW-001 | `cart_items` | Cart quantity after PUT qty=2 | quantity=2, updated_at=2026-09-30 09:43:36 | **PASS** |
| DB-NEW-002 | `cart_items` | Cart after DELETE item | 0 rows (item physically deleted) | **PASS** |
| DB-011 | all test tables | Test data summary counts | 3 users, 1 address, 3 orders, 4 payments, 0 cart_items | **PASS** |

**DB-001 note:** The `reward_points` value captured at query time (2026-09-30T06:45:42Z)
is 39, reflecting purchases made after registration. At registration, reward_points was 0
(confirmed by CHK-005 which captured `rewardPoints=0` immediately after account creation).

---

## Business Rules Verified

| Rule | Evidence | Result |
|---|---|---|
| Registration returns JWT + ROLE_CUSTOMER | CHK-001: HTTP 201, token, role=ROLE_CUSTOMER | **PASS** |
| Login returns JWT on valid credentials | CHK-002: HTTP 200, token | **PASS** |
| Duplicate registration returns 409 | CHK-003: HTTP 409, urn:problem:username-already-exists | **PASS** |
| Flat $5.00 shipping applied | CHK-023: totalAmount=44.50 ($39.50 book + $5.00) | **PASS** |
| Stock decremented on PAID order | DB-006: 18→17 after payment of order 2c8616ea | **PASS** |
| Reward points = floor(book_subtotal) | DB-007: floor(39.50)=39 pts (shipping excluded) | **PASS** |
| Cart cleared after payment | CHK-026 post: cart items=0; DB-008: 0 cart_items rows | **PASS** |
| Stock restored on PAID order cancel | DB-006: 17→18→17 (cancel order1, pay order2) | **PASS** |
| Reward points reversed on PAID order cancel | rewardPoints: 39→0 (order1 cancel), then 39 (order2 pay) | **PASS** |
| PENDING order cancel: stock unchanged | DB-006: no change for order 271be846 (never paid) | **PASS** |
| canBeCancelled=false after cancellation | CHK-027: canBeCancelled=false | **PASS** |
| buy-again excludes cancelled orders | CHK-028/SCH-006: only order 2c8616ea (PAID) appears | **PASS** |
| Simulated INSUFFICIENT_FUNDS → 400 | CHK-030: HTTP 400 with detail message | **PASS** |

---

## Failures

**None.** All 23 operations were exercised via live HTTP. Seven additional response-schema checks (SCH-001 – SCH-007) passed.

---

## Test Data Left in Database

| Entity | Count | Details |
|---|---|---|
| users | 3 | captest001 (dce3cc76-...), captest002 (b05845e0-...), verifytest001 (f707cafa-...) |
| user_addresses | 1 | captest001: id=5818f14f-..., 123 Test Street, Testville |
| orders | 3 | All captest001: adf03c81 (CANCELLED), 2c8616ea (PAID), 271be846 (CANCELLED) |
| payments | 4 | 9079da2b (SUCCESS), f542c7eb (SUCCESS), a6a764df (FAILED), d49cc126 (FAILED) |
| cart_items | 0 | Cart cleared; final added item (fec751dc) deleted during schema checks |

Pre-existing users (`customer_demo`, `smokeuser`, `journey_1790741524`,
`cartcheck_1790741545`) and their data were not modified.

---

## Limitations & Notes

1. **Port 8080 occupied**: An `oc port-forward` process holds `localhost:8080`.
   Tests ran on port 8081. Same codebase, same database — no functional difference.

2. **JWT_SECRET**: The pre-running instance (PID 96998) had an empty `JWT_SECRET`.
   A new instance was started with a proper secret per project README. No code changed.

3. **Cart history note**: After the successful payment of order 2c8616ea, the cart was
   cleared (0 items). A subsequent Clean Architecture add (item fec751dc) was made for
   the schema validation runs (SCH-004/005) and later removed; DB-NEW-002 confirms 0
   rows in `cart_items` after that deletion. No residual cart items remain.
