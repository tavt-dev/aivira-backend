# Aivira Backend Completion Plan

## Status Legend

- `[x]` Done in the current backend.
- `[ ]` Still needs implementation.
- Removed from this plan: anything that reintroduces seller/shop/merchant behavior, provider refund integration, mandatory audit-table work, separate `Book` tables, separate author tables, and complex analytics tables.

## Current Backend Baseline

The repository is already a single-vendor bookstore backend foundation. `Product` is the backend domain name, but it represents a sellable book. Keep `/products` and `Product*` names for API and code stability.

Already done:

- [x] Single-vendor direction documented. No active seller/shop/merchant route family exists.
- [x] Active roles are `USER` and `ADMIN`.
- [x] Hybrid RBAC exists with roles, permissions, and direct user permissions.
- [x] Auth exists: register, verify email, login, refresh token, logout, logout all, password reset, active sessions.
- [x] User profile and address book exist.
- [x] Public category APIs exist: `GET /categories`, `GET /categories/tree`.
- [x] Admin category APIs exist under `/admin/categories/**`.
- [x] Public product APIs exist: `GET /products`, `GET /products/{slug}`.
- [x] Admin product APIs exist under `/admin/products/**`.
- [x] Product media upload/update/delete exists.
- [x] Product variation and stock management exists.
- [x] Cart APIs exist.
- [x] Checkout exists and creates one order per checkout request.
- [x] Customer order list/detail/cancel exists.
- [x] COD, VNPay, and MoMo provider flows exist.
- [x] Payment retry, callback/IPN idempotency, expiry, and reconciliation exist.
- [x] Coupon, promotion, review, and review image tables/entities exist as foundation.
- [x] Flyway is the schema source of truth.
- [x] `ddl-auto=validate` is used in normal profiles.
- [x] Basic OpenAPI configuration exists.
- [x] Actuator health is exposed.
- [x] Brute-force login lockout exists.

Still missing for a complete backend:

- [x] Book-specific catalog fields and search.
- [x] Admin order lifecycle.
- [x] Admin user list/detail/lock/unlock/role assignment.
- [x] Checkout preview and coupon application logic.
- [x] Coupon and promotion admin APIs.
- [x] Manual refund metadata and admin refund action.
- [ ] Review APIs and moderation.
- [ ] Storefront home API and admin dashboard APIs.
- [ ] Demo bookstore seed data.
- [ ] API docs and README alignment for the final backend.
- [ ] Production-practical polish around logs, env docs, deployment notes, and test coverage.

## Phase 1: Book Catalog Completion

Goal: finish the bookstore-specific catalog model while keeping current `Product` and `/products` naming.

### Phase 1 Current Status

- [x] `Product` entity exists.
- [x] `ProductCreateRequest`, `ProductUpdateRequest`, and `ProductResponse` exist.
- [x] `ProductMapper` exists.
- [x] `ProductService` and `ProductServiceImpl` exist.
- [x] `ProductSpecifications` exists.
- [x] `ProductRepository` exists.
- [x] Public product list/detail endpoints exist.
- [x] Admin product CRUD/media/variation/stock endpoints exist.
- [x] Product status policy exists.
- [x] Product stock is recalculated from active variations.
- [x] Admin-created products are active immediately.
- [x] Admin soft delete sets `status=INACTIVE` and `active=false`.
- [x] Book metadata fields are implemented.
- [x] Author/publisher/ISBN search is implemented.
- [x] `name_asc` sort is implemented.

### Phase 1 Scope

Implement these book fields directly on `products`:

- [x] `book_author`
- [x] `isbn`
- [x] `publisher`
- [x] `publication_year`
- [x] `book_language`
- [x] `page_count`
- [x] `book_format`
- [x] `dimensions`

Do not implement in Phase 1:

- Separate `books` table.
- Separate `authors` table.
- `product_authors` join table.
- Full-text search engine.
- Book reviews or ratings.
- Promotions, coupons, or storefront sections.

### Phase 1 Schema

Add a new Flyway migration:

- [x] `src/main/resources/db/migration/V6__book_catalog_fields.sql`

Migration requirements:

- [x] Add `book_author VARCHAR(255) NOT NULL DEFAULT 'Unknown'` to `products`.
- [x] Add `isbn VARCHAR(20) NULL` to `products`.
- [x] Add `publisher VARCHAR(255) NULL` to `products`.
- [x] Add `publication_year INT NULL` to `products`.
- [x] Add `book_language VARCHAR(80) NULL` to `products`.
- [x] Add `page_count INT NULL` to `products`.
- [x] Add `book_format VARCHAR(50) NOT NULL DEFAULT 'PAPERBACK'` to `products`.
- [x] Add `dimensions VARCHAR(120) NULL` to `products`.
- [x] Add unique index `uk_products_isbn` on `isbn`.
- [x] Add `idx_products_book_author`.
- [x] Add `idx_products_publisher`.
- [x] Add `idx_products_publication_year`.

Compatibility:

- [x] Existing rows get `book_author='Unknown'` and `book_format='PAPERBACK'`.
- [x] New admin create requests must still require a real `bookAuthor`.

### Phase 1 Domain And DTOs

Add enum:

- [x] `BookFormat`
- [x] Values: `PAPERBACK`, `HARDCOVER`, `EBOOK`, `BOXSET`, `OTHER`

Update `Product`:

- [x] Add `bookAuthor`.
- [x] Add `isbn`.
- [x] Add `publisher`.
- [x] Add `publicationYear`.
- [x] Add `bookLanguage`.
- [x] Add `pageCount`.
- [x] Add `bookFormat`.
- [x] Add `dimensions`.

Keep for backward compatibility:

- [x] `brand`
- [x] `material`
- [x] `weight`

Update request/response DTOs:

- [x] `ProductCreateRequest` includes new fields.
- [x] `ProductUpdateRequest` includes new fields.
- [x] `ProductResponse` returns new fields.

Validation:

- [x] `bookAuthor` is required on create.
- [x] `isbn` max length is 20.
- [x] `isbn` is optional but unique when present.
- [x] `publisher` max length is 255.
- [x] `publicationYear` must be from `1000` to current year + 1.
- [x] `bookLanguage` max length is 80.
- [x] `pageCount` must be positive when present.
- [x] `bookFormat` defaults to `PAPERBACK`.
- [x] `dimensions` max length is 120.

### Phase 1 Repository And Service

Update `ProductRepository`:

- [x] Add `existsByIsbn(String isbn)`.
- [x] Add `existsByIsbnAndIdNot(String isbn, Long id)`.

ISBN rule:

- [x] Store ISBN as trimmed input.
- [x] Convert blank ISBN to null.
- [x] Do not remove hyphens or spaces in this phase.

Update `ProductService`:

- [x] Extend `getPublicProducts` with `author`, `publisher`, and `isbn`.

Update `ProductServiceImpl` create flow:

- [x] Validate product SKU uniqueness.
- [x] Resolve and validate slug.
- [x] Create admin product as active immediately.
- [x] Set `approvedBy` and `approvedAt`.
- [x] Create variations.
- [x] Recalculate stock.
- [x] Require at least one active variation.
- [x] Validate ISBN uniqueness.
- [x] Validate publication year.
- [x] Validate page count.
- [x] Store book metadata.
- [x] Default missing `bookFormat` to `PAPERBACK`.

Update `ProductServiceImpl` update flow:

- [x] Update SKU with uniqueness validation.
- [x] Update product name and slug.
- [x] Update category, price, brand, material, weight, featured.
- [x] Keep active product active.
- [x] Update book metadata.
- [x] Validate ISBN uniqueness on change.
- [x] Allow clearing optional book metadata with blank string.
- [x] Reject blank `bookAuthor` when provided.

### Phase 1 Search And API

Update `GET /products`:

- [x] Supports `keyword`.
- [x] Supports `categorySlug`.
- [x] Supports `brand`.
- [x] Supports `minPrice`.
- [x] Supports `maxPrice`.
- [x] Supports `available`.
- [x] Supports `sort`, `page`, and `size`.
- [x] Add `author`.
- [x] Add `publisher`.
- [x] Add `isbn`.

Update sort:

- [x] `newest`.
- [x] `price_asc`.
- [x] `price_desc`.
- [x] `best_selling`.
- [x] `name_asc`.

Update keyword search:

- [x] Matches product name.
- [x] Matches SKU.
- [x] Matches description.
- [x] Matches brand.
- [x] Matches author.
- [x] Matches publisher.
- [x] Matches ISBN.

### Phase 1 Tests

Update or add tests:

- [x] `ProductServiceImplTest`: create stores book metadata.
- [x] `ProductServiceImplTest`: create defaults `bookFormat`.
- [x] `ProductServiceImplTest`: duplicate ISBN fails.
- [x] `ProductServiceImplTest`: blank author fails.
- [x] `ProductServiceImplTest`: invalid publication year fails.
- [x] `ProductServiceImplTest`: invalid page count fails.
- [x] `ProductServiceImplTest`: update ISBN validates uniqueness.
- [x] `ProductServiceImplTest`: update can clear optional metadata.
- [x] `CatalogPublicIntegrationTest`: migration has new columns.
- [x] `CatalogPublicIntegrationTest`: public search by author.
- [x] `CatalogPublicIntegrationTest`: public search by publisher.
- [x] `CatalogPublicIntegrationTest`: public search by ISBN.
- [x] `CatalogPublicIntegrationTest`: `name_asc` sorting.
- [x] `ProductControllerContractTest`: public list accepts new params.

Run:

- [x] `.\mvnw.cmd -Dtest=ProductServiceImplTest test`
- [x] `.\mvnw.cmd -Dtest=CatalogPublicIntegrationTest test` completed with Testcontainers skipped because Docker is unavailable.
- [x] `.\mvnw.cmd test`

### Phase 1 Acceptance Criteria

- [x] Flyway migration adds all book metadata fields and indexes.
- [x] Hibernate validate succeeds in compiled test context; Docker-backed migration validation is covered by skipped Testcontainers tests.
- [x] Admin can create a book with metadata.
- [x] Admin can update book metadata.
- [x] ISBN uniqueness is enforced.
- [x] Public search supports author, publisher, and ISBN.
- [x] Keyword search includes author, publisher, and ISBN.
- [x] Existing media, variation, cart, checkout, order, and payment behavior is unchanged.
- [x] Targeted and full tests pass, with Docker-backed tests skipped locally.

## Phase 2: Admin Order Lifecycle

Current status:

- [x] Customer order list exists.
- [x] Customer order detail exists.
- [x] Customer cancellation exists for safe early states.
- [x] Customer cancellation restores stock when allowed.
- [x] Paid customer cancellation is blocked and requires refund flow.
- [x] Admin order list/detail exists.
- [x] Admin order status transition endpoints exist.

Implement:

- [x] `GET /admin/orders`
- [x] `GET /admin/orders/{orderId}`
- [x] `PUT /admin/orders/{orderId}/confirm`
- [x] `PUT /admin/orders/{orderId}/packing`
- [x] `PUT /admin/orders/{orderId}/shipping`
- [x] `PUT /admin/orders/{orderId}/completed`
- [x] `PUT /admin/orders/{orderId}/cancel`

Admin filters:

- [x] `status`
- [x] `keyword`
- [x] `fromDate`
- [x] `toDate`
- [x] `page`
- [x] `size`

Transition rules:

- [x] `PENDING_CONFIRMATION -> CONFIRMED`
- [x] `PAID -> CONFIRMED`
- [x] `CONFIRMED -> PACKING`
- [x] `PACKING -> SHIPPING`
- [x] `SHIPPING -> COMPLETED`
- [x] `PENDING_CONFIRMATION -> CANCELLED`
- [x] `CONFIRMED -> CANCELLED`
- [x] `PACKING -> CANCELLED`

Keep:

- [x] Restore stock when admin cancels before shipping.
- [x] Block cancellation after `SHIPPING`.
- [x] Block simple cancellation for paid orders until manual refund metadata exists.
- [x] Protect list/detail with `ORDER_MANAGE_ALL` or `ORDER_READ_ALL`.
- [x] Protect status transitions with `ORDER_MANAGE_ALL` or `ORDER_UPDATE_STATUS_ALL`.
- [x] Protect admin cancel with `ORDER_MANAGE_ALL` or `ORDER_CANCEL_ALL`.

## Phase 3: Admin User And Permission Management

Current status:

- [x] `GET /admin/permissions` exists.
- [x] `GET /admin/roles` exists.
- [x] `GET /admin/roles/{roleCode}/permissions` exists.
- [x] `PUT /admin/roles/{roleCode}/permissions` exists.
- [x] `GET /admin/users/{userId}/permissions` exists.
- [x] `POST /admin/users/{userId}/permissions` exists.
- [x] `DELETE /admin/users/{userId}/permissions/{permissionCode}` exists.
- [x] Direct user permission service exists.
- [x] Role/permission service exists.
- [x] Admin user list/detail exists.
- [x] Admin user lock/unlock exists.
- [x] Admin role assignment endpoint exists.

Remaining endpoints:

- [x] `GET /admin/users`
- [x] `GET /admin/users/{userId}`
- [x] `PUT /admin/users/{userId}/lock`
- [x] `PUT /admin/users/{userId}/unlock`
- [x] `PUT /admin/users/{userId}/roles`

Rules:

- [x] Keep only `USER` and `ADMIN` as active predefined roles.
- [x] Do not add seller/shop roles.
- [x] Admin can lock/unlock accounts.
- [x] Admin can assign roles.
- [x] Admin can inspect effective/direct permissions.
- [x] Admin can grant/revoke direct user permissions.

Removed from this phase:

- Mandatory audit-table implementation. Add logs first; introduce audit table only if required later.

## Phase 4: Coupon And Promotion Support

Current status:

- [x] `coupons` table exists.
- [x] `coupon_usages` table exists.
- [x] `promotions` table exists.
- [x] `Coupon`, `CouponUsage`, and `Promotion` entities exist.
- [x] `Order.couponCode` exists.
- [x] `OrderItem.discountAmount` and `OrderItem.promotionName` exist.
- [x] Coupon repositories/services/controllers exist.
- [x] Promotion repositories/services/controllers exist.
- [x] Checkout preview exists.
- [x] Checkout coupon application exists.
- [x] Coupon usage lifecycle supports `RESERVED`, `FINALIZED`, and `RELEASED`.

Implement:

- [x] `POST /checkout/preview`
- [x] Let `POST /checkout` accept optional `couponCode`.
- [x] `GET /admin/coupons`
- [x] `POST /admin/coupons`
- [x] `GET /admin/coupons/{couponId}`
- [x] `PUT /admin/coupons/{couponId}`
- [x] `DELETE /admin/coupons/{couponId}`
- [x] `GET /admin/promotions`
- [x] `POST /admin/promotions`
- [x] `GET /admin/promotions/{promotionId}`
- [x] `PUT /admin/promotions/{promotionId}`
- [x] `DELETE /admin/promotions/{promotionId}`

Rules:

- [x] Promotion applies before coupon.
- [x] Total amount cannot go below zero.
- [x] COD checkout finalizes coupon usage immediately.
- [x] Online checkout reserves coupon usage first.
- [x] Online payment success finalizes coupon usage exactly once.
- [x] Failed, expired, retried, or cancelled online payments do not permanently consume coupon usage.

## Phase 5: Manual Refund Flow

Current status:

- [x] `PaymentStatus.REFUNDED` exists.
- [x] `OrderStatus.REFUNDED` exists.
- [x] Paid customer cancellation is blocked with `ORDER_CANCEL_REQUIRES_REFUND`.
- [x] Refund metadata exists in dedicated `refunds` table.
- [x] Admin refund endpoint exists.

Implement:

- [x] Add migration `V8__manual_refunds.sql`.
- [x] Add required request DTO with refund amount, reason, and note.
- [x] Add `PUT /admin/orders/{orderId}/mark-refunded`.
- [x] Mark related payment records as `REFUNDED`.
- [x] Mark related payment group as `REFUNDED`.
- [x] Mark order as `REFUNDED`.
- [x] Restore stock for allowed pre-shipping paid refund states.
- [x] Log the admin user, order, refund code, and amount.

Removed from this phase:

- Provider refund API integration for VNPay or MoMo.
- Optional `GET /admin/refunds` list until refund volume justifies it.

## Phase 6: Reviews And Ratings

Current status:

- [x] `reviews` table exists.
- [x] `review_images` table exists.
- [x] `Review` and `ReviewImage` entities exist.
- [x] Product and User relationships to reviews exist.
- [x] Review repositories/controllers/services exist.
- [x] Public review list exists.
- [x] Customer review create/update/delete exists.
- [x] Admin review moderation/reply exists.

Implement:

- [x] `GET /products/{slug}/reviews`
- [x] `POST /orders/{orderId}/items/{orderItemId}/review`
- [x] `PUT /reviews/{reviewId}`
- [x] `DELETE /reviews/{reviewId}`
- [x] `GET /admin/reviews`
- [x] `PUT /admin/reviews/{reviewId}/moderate`
- [x] `PUT /admin/reviews/{reviewId}/reply`

Rules:

- [x] User can review only purchased books.
- [x] Order must be `COMPLETED`.
- [x] One review per order item.
- [x] Public only sees approved/visible reviews.
- [x] Admin reply uses `admin_reply`, not shop reply.

## Phase 7: Storefront And Dashboard APIs

Current status:

- [x] Product has `featured`.
- [x] Product has `soldCount`.
- [x] Product has `stockQuantity`.
- [x] Order and payment statuses exist.
- [x] Storefront home endpoint exists.
- [x] Admin dashboard endpoints exist.

Implement:

- [x] `GET /storefront/home`
- [x] `GET /admin/dashboard/summary`
- [x] `GET /admin/dashboard/sales`
- [x] `GET /admin/dashboard/orders`
- [x] `GET /admin/dashboard/top-books`
- [x] `GET /admin/dashboard/low-stock`

Dashboard metrics:

- [x] Revenue by date range.
- [x] Order count by status.
- [x] Payment success/failure split.
- [x] Top selling books.
- [x] Low stock books.
- [x] New users.
- [x] Pending orders.
- [x] Pending payments.

Removed from this phase:

- Complex analytics tables. Use live queries first.

## Phase 8: Seed Data And Developer Experience

Current status:

- [x] Admin seed support exists behind `app.seed.enabled`.
- [x] `SEED_ENABLED` and `SEED_ADMIN_*` are documented in config.
- [ ] Demo bookstore catalog seed is missing.

Implement:

- [ ] Add `SEED_DEMO_CATALOG_ENABLED`.
- [ ] Seed root and child categories.
- [ ] Seed 20-50 books after Phase 1 fields exist.
- [ ] Seed default variations and stock.
- [ ] Seed cover image URLs.
- [ ] Seed featured books.
- [ ] Keep demo seed disabled by default.
- [ ] Document seed behavior in README.

## Phase 9: API Documentation And Error Contract

Current status:

- [x] OpenAPI config exists.
- [x] Controllers have basic OpenAPI annotations.
- [x] `ApiResponse` and `PageResponse` are used.
- [x] Domain error-code enums exist.
- [ ] New book field docs are missing.
- [ ] Admin order transition docs are missing.
- [ ] Coupon/promotion docs are missing.
- [ ] Manual refund docs are missing.
- [ ] Review/dashboard docs are missing.
- [ ] Some planned error codes are missing.

Implement missing error codes as features are built:

- [ ] ISBN already exists.
- [ ] Invalid publication year.
- [ ] Invalid page count.
- [ ] Invalid order transition.
- [x] Paid order requires refund.
- [x] Coupon invalid/expired/used.
- [x] Review not allowed.
- [x] Refund not allowed.

## Phase 10: Production-Practical Hardening

Current status:

- [x] Brute-force login lockout exists.
- [x] CORS is environment-driven.
- [x] Refresh token cookie config is environment-driven.
- [x] Flyway is enabled.
- [x] `ddl-auto=validate` is configured.
- [x] Actuator health is enabled.
- [x] Payment callback/IPN idempotency exists.
- [x] Payment amount verification exists.
- [x] Payment expiry scheduler exists.
- [ ] Deployment checklist is missing.
- [ ] Backup/restore notes are missing.
- [ ] Final env-var documentation is incomplete.
- [ ] CI workflow is not present in repo.

Implement:

- [ ] Document required env vars in README.
- [ ] Document local/dev/prod config differences.
- [ ] Add deployment checklist.
- [ ] Add MySQL backup/restore notes.
- [ ] Add CI workflow if this repo will be hosted with CI.
- [ ] Review logs to ensure OTPs, tokens, and secrets are not logged.

## Suggested Implementation Order

1. [x] Phase 1: Book catalog fields and search.
2. [x] Phase 2: Admin order lifecycle.
3. [x] Phase 3: Admin user and permission management.
4. [x] Phase 4: Checkout preview and coupon foundation.
5. [x] Phase 4: Coupon/promotion admin APIs.
6. [x] Phase 5: Manual refund flow.
7. [x] Phase 6: Review and moderation.
8. [x] Phase 7: Storefront and dashboard APIs.
9. [ ] Phase 8: Demo seed data.
10. [ ] Phase 9: API docs and README cleanup.
11. [ ] Phase 10: Production hardening and CI polish.

## Completion Criteria

Backend is complete for the full commerce version when:

- [ ] Public users can browse, search, filter, and view book details.
- [ ] Customers can register, verify email, log in, manage profile and addresses.
- [x] Customers can add books to cart, preview checkout, apply coupon, checkout, pay, and track orders.
- [x] COD, VNPay, and MoMo flows work with retry, expiry, callback/IPN, and reconciliation.
- [ ] Admin can manage books, categories, media, variations, stock, orders, coupons, promotions, reviews, users, permissions, payments, refunds, and dashboard.
- [x] Paid-order refund is handled manually with clear admin metadata.
- [x] No seller/shop/merchant workflow exists.
- [x] Schema changes use Flyway migrations.
- [x] API responses use DTOs, not JPA entities.
- [x] `.\mvnw.cmd test` passes for the current implemented backend.
- [ ] README, PLAN, and OpenAPI docs match the implemented backend behavior.

## Explicit Defaults

- [x] Scope: full commerce backend.
- [x] Production readiness target: practical production baseline.
- [x] Refund approach: manual refund, no provider refund integration yet.
- [x] Shipping fee: keep simple and default to zero until a shipping module is introduced.
- [x] Author model: store author as a string on `products`.
- [x] Backend route naming: keep `/products` for stability.
- [x] UI/product language: call products "books".
- [x] Multi-vendor: explicitly out of scope.
