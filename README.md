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

- Auth: register, verify email, login, Google OAuth login, refresh token, logout, password reset, active sessions.
- User profile and address book.
- Hybrid RBAC with roles, permissions, and direct user permissions.
- Public catalog: categories, product search, product detail.
- Admin catalog: create/update/soft-delete products, manage media, variations, and stock.
- Cart, checkout preview, coupon/promotion discounts, one-order checkout creation, payment groups, payment callbacks, retry, and reconciliation.
- Admin orders, users, manual refunds, reviews, storefront home, and dashboard APIs.
- Admin-managed news/blog with categories, sanitized rich-text content, SEO metadata, Cloudinary images, and related books.

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
- `GET /blog/posts`
- `GET /blog/posts/{slug}`
- `GET /blog/categories`
- `GET /auth/google/authorize`
- `GET /auth/google/callback`
- `POST /auth/google/exchange-ticket`

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
- `/admin/categories/**`
- `/admin/orders/**`
- `/admin/users/**`
- `/admin/coupons/**`
- `/admin/promotions/**`
- `/admin/reviews/**`
- `/admin/dashboard/**`
- `/admin/permissions/**`
- `/admin/roles/**`
- `/admin/payments/**`
- `/admin/blog/**`

## API Behavior Notes

- The backend resource name remains `Product`, but products are books in the business domain and UI language.
- `/products` is the stable public catalog route for browsing, searching, filtering, and viewing book details.
- Public product search supports book metadata filters such as `author`, `publisher`, `isbn`, and `name_asc` sorting.
- `POST /checkout/preview` is non-mutating: it validates cart/address/inventory/discounts but does not create orders, payments, stock locks, or coupon usage.
- Promotions apply before coupons. Product/category promotions are item-level; coupons are order-level after promotion discounts.
- COD checkout finalizes coupon usage immediately. Online checkout reserves coupon usage and finalizes or releases it from payment success/failure/expiry.
- Manual refund records admin metadata and marks order/payment refunded without calling VNPay or MoMo refund APIs.
- Reviews require a completed purchased order item. Public review lists show only approved, visible, non-deleted reviews.
- Demo catalog seed is disabled by default and must not be enabled in production.

## Migrations

- `V1__init_schema.sql`: base schema and RBAC reference data.
- `V3__catalog_phase4.sql`: catalog relationship fields.
- `V4__phase5_checkout_payment.sql`: checkout/payment hardening tables and fields.
- `V5__payment_provider_hardening.sql`: provider attempt/reconciliation support.
- `V6__book_catalog_fields.sql`: bookstore metadata fields on products.
- `V7__coupon_usage_status.sql`: coupon usage lifecycle for online payment safety.
- `V8__manual_refunds.sql`: manual refund metadata table.
- `V9__review_order_item_visibility.sql`: review order item ownership, visibility, and moderation metadata.
- `V10__google_oauth_login.sql`: one-time Google OAuth state and login-ticket tables.
- `V12__blog_news_module.sql`: blog categories, posts, rich-text assets, and related-book links.

## Seed Data

Seed behavior is controlled by environment variables and is disabled by default:

- `SEED_ENABLED=false`: master switch. When false, no seed runner is created.
- `SEED_ADMIN_USERNAME`, `SEED_ADMIN_PASSWORD`, `SEED_ADMIN_EMAIL`: optional default admin credentials.
- `SEED_DEMO_CATALOG_ENABLED=false`: optional demo bookstore catalog seed.

When `SEED_ENABLED=true`, the app seeds default permissions and tries to create the configured admin account. When `SEED_DEMO_CATALOG_ENABLED=true`, it also creates an idempotent demo bookstore catalog with categories, 30 books, default variations, stock quantities, cover image URLs, featured books, and sample bestselling counts.

Do not enable demo catalog seed in production. Re-running the seed does not duplicate categories, books, variations, or media, and existing books with matching SKU or slug are preserved.

## Environment Variables

Product view tracking requires `PRODUCT_VIEW_HASH_PEPPER` in production. Use a long random secret that is different
from the JWT signing key. View history APIs include public `POST /products/{slug}/views` and authenticated
`/users/me/recently-viewed/**` operations.

Practical local/dev configuration is environment-driven:

- Database: `DB_URL`, `USERNAME_DB`, `PASSWORD_DB`.
- JWT: `JWT_SIGNER_KEY`, access/refresh expiry settings.
- Mail/OTP: SMTP host, port, username, password, sender, and OTP expiry settings.
- Google OAuth: `GOOGLE_OAUTH_ENABLED`, `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`, `GOOGLE_OAUTH_REDIRECT_URI`, `GOOGLE_OAUTH_FRONTEND_SUCCESS_URL`, and `GOOGLE_OAUTH_FRONTEND_FAILURE_URL`. The Google Console authorized redirect URI must exactly match `GOOGLE_OAUTH_REDIRECT_URI`.
- Cloudinary: cloud name, API key, API secret, upload folder/preset settings.
- Payment providers: VNPay and MoMo enable flags, merchant ids/codes, secret keys, callback/IPN/return URLs, and expiry settings.
- Seed: `SEED_ENABLED`, `SEED_ADMIN_USERNAME`, `SEED_ADMIN_PASSWORD`, `SEED_ADMIN_EMAIL`, `SEED_DEMO_CATALOG_ENABLED`.
- Browser integration: CORS allowed origins and refresh-cookie secure/same-site/domain settings.

Production should provide secrets through the deployment environment, not committed config files. `JWT_SIGNER_KEY` and provider secrets must be treated as required outside test/local-only setups.

## Error Contract

Expected business errors are returned through the same API envelope shape:

```json
{
  "success": false,
  "errorCode": "PRODUCT-409-003",
  "message": "Product ISBN already exists",
  "data": null,
  "timestamp": 1766558400000
}
```

Important domain error groups:

- Catalog: duplicate SKU/slug/ISBN, invalid publication year, invalid page count, missing author, missing category, invalid variation, and media validation failures.
- Checkout/payment/coupon: invalid cart or checkout state, invalid/expired/over-limit coupon, coupon minimum order not met, provider disabled, invalid callback/signature, payment retry not allowed, and reconciliation failures.
- Orders/refunds: order not found, invalid status transition, cancellation not allowed, paid cancellation requiring refund, refund not allowed, refund already processed, and invalid refund amount.
- Reviews: review not found, review not allowed, duplicate order-item review, order not completed, deleted review, and invalid review image metadata.
- Auth/RBAC: authentication failure, access denied, locked/deleted account, invalid/expired JWT, OTP errors, role errors, permission errors, and Google OAuth state/ticket/token validation errors.

Success responses use `ApiResponse<T>`. Paginated list responses use `ApiResponse<PageResponse<T>>`.

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

## Test

```powershell
.\mvnw.cmd test
```

Integration tests use Testcontainers and are skipped when Docker is unavailable.

## Code Formatting

To format the Java code using the `formatter-maven-plugin`:

```powershell
.\mvnw.cmd formatter:format
```

To validate that the Java code matches the formatter rules:

```powershell
.\mvnw.cmd formatter:validate
```

## Frontend

The React/Vite application is maintained in the separate
[aivira-frontend](https://github.com/tavt-dev/aivira-frontend) repository. This repository contains only the Spring Boot backend.

For browser integration, configure the deployed frontend origin in `CORS_ALLOWED_ORIGINS`. Google OAuth success/failure URLs and VNPay/MoMo browser return URLs must point to frontend routes, while provider callback/IPN URLs must point to this backend.
