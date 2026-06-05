# Aivira Backend

Spring Boot backend for the Aivira single-vendor online bookstore. Aivira/Admin manages the catalog directly, while customers browse books, manage carts, checkout, pay, and track orders.

## Stack

- Java 21
- Spring Boot 4
- Spring Web, Spring Data JPA, Spring Security
- MySQL, Flyway
- JWT, refresh-token sessions, OTP email flows
- Cloudinary uploads
- COD, VNPay, and MoMo payment flows
- JUnit 5, Mockito, AssertJ, Testcontainers

## Current Scope

- Auth: register, verify email, login, refresh token, logout, password reset, active sessions.
- User profile and address book.
- Hybrid RBAC with roles, permissions, and direct user permissions.
- Public catalog: categories, product search, product detail.
- Admin catalog: create/update/soft-delete products, manage media, variations, and stock.
- Cart, checkout preview, coupon/promotion discounts, one-order checkout creation, payment groups, payment callbacks, retry, and reconciliation.
- Admin orders, users, manual refunds, reviews, storefront home, and dashboard APIs.

## Active Roles

- `USER`
- `ADMIN`

## Important Direction

This project is now single-vendor. Do not reintroduce customer-facing external contributor onboarding, seller/shop APIs, seller/shop permissions, shop ownership columns, platform shop tables, or multi-merchant flows.

## Main Endpoints

Public:

- `GET /products`
- `GET /products/{slug}`
- `GET /products/{slug}/reviews`
- `GET /categories`
- `GET /categories/tree`
- `GET /storefront/home`

Customer:

- `/auth/**`
- `/users/me/**`
- `/users/me/addresses/**`
- `/cart/**`
- `/checkout`
- `/orders/**`
- `/payments/**`
- `/reviews/**`

Admin:

- `/admin/products/**`
- `/admin/orders/**`
- `/admin/users/**`
- `/admin/coupons/**`
- `/admin/promotions/**`
- `/admin/reviews/**`
- `/admin/dashboard/**`
- `/admin/permissions/**`
- `/admin/roles/**`
- `/admin/payments/**`

## Migrations

- `V1__init_schema.sql`: base schema and RBAC reference data.
- `V3__catalog_phase4.sql`: catalog relationship fields.
- `V4__phase5_checkout_payment.sql`: checkout/payment hardening tables and fields.
- `V5__payment_provider_hardening.sql`: provider attempt/reconciliation support.
- `V6__book_catalog_fields.sql`: bookstore metadata fields on products.
- `V7__coupon_usage_status.sql`: coupon usage lifecycle for online payment safety.
- `V8__manual_refunds.sql`: manual refund metadata table.
- `V9__review_order_item_visibility.sql`: review order item ownership, visibility, and moderation metadata.

## Seed Data

Seed behavior is controlled by environment variables and is disabled by default:

- `SEED_ENABLED=false`: master switch. When false, no seed runner is created.
- `SEED_ADMIN_USERNAME`, `SEED_ADMIN_PASSWORD`, `SEED_ADMIN_EMAIL`: optional default admin credentials.
- `SEED_DEMO_CATALOG_ENABLED=false`: optional demo bookstore catalog seed.

When `SEED_ENABLED=true`, the app seeds default permissions and tries to create the configured admin account. When `SEED_DEMO_CATALOG_ENABLED=true`, it also creates an idempotent demo bookstore catalog with categories, 30 books, default variations, stock quantities, cover image URLs, featured books, and sample bestselling counts.

Do not enable demo catalog seed in production. Re-running the seed does not duplicate categories, books, variations, or media, and existing books with matching SKU or slug are preserved.

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

## Test

```powershell
.\mvnw.cmd test
```

Integration tests use Testcontainers and are skipped when Docker is unavailable.
