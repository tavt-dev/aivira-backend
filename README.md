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
- Cart, checkout, one-order checkout creation, payment groups, payment callbacks, retry, and reconciliation.

## Active Roles

- `USER`
- `ADMIN`

## Important Direction

This project is now single-vendor. Do not reintroduce customer-facing external contributor onboarding, seller/shop APIs, seller/shop permissions, shop ownership columns, platform shop tables, or multi-merchant flows.

## Main Endpoints

Public:

- `GET /products`
- `GET /products/{slug}`
- `GET /categories`
- `GET /categories/tree`

Customer:

- `/auth/**`
- `/users/me/**`
- `/users/me/addresses/**`
- `/cart/**`
- `/checkout`
- `/orders/**`
- `/payments/**`

Admin:

- `/admin/products/**`
- `/admin/permissions/**`
- `/admin/roles/**`
- `/admin/payments/**`

## Migrations

- `V1__init_schema.sql`: base schema and RBAC reference data.
- `V3__catalog_phase4.sql`: catalog relationship fields.
- `V4__phase5_checkout_payment.sql`: checkout/payment hardening tables and fields.
- `V5__payment_provider_hardening.sql`: provider attempt/reconciliation support.

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

## Test

```powershell
.\mvnw.cmd test
```

Integration tests use Testcontainers and are skipped when Docker is unavailable.
