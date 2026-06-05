# Aivira Frontend Completion Plan

## Summary

Hoan thien frontend Aivira thanh mot bookstore web app day du, bam sat backend hien tai. Frontend tiep tuc dung React 18, Vite, TailwindCSS, React Router, i18next, lucide-react va api layer san co trong `frontend/src/api`.

Muc tieu:

- Khach hang co the browse/search/filter sach, xem chi tiet, gio hang, checkout preview, coupon, thanh toan, theo doi don, review sach da mua.
- Admin co the quan ly catalog, categories, media, variations, stock, orders, refunds, users, permissions, coupons, promotions, reviews, payments, dashboard.
- UI goi san pham la "books", nhung API route giu `/products`.
- Khong them seller/shop/merchant workflow.
- Khong dua business rule phuc tap vao component; frontend chi validate UX-level, backend van la source of truth.

## Current Frontend Baseline

Tech stack:

- React 18 + Vite.
- React Router.
- TailwindCSS.
- i18next/react-i18next.
- lucide-react icons.
- `fetch` wrapper in `src/api/client.js`.
- Local storage helpers in `src/utils/storage.js`.
- Existing theme/motion utilities.

Already exists:

- Public layout, navbar, footer, intro animation, dark mode/theme support.
- Auth modal for login/register flow.
- Public pages:
  - `HomePage`
  - `CategoryPage`
  - `ProductPage`
  - `CartPage`
  - `CheckoutPage`
  - `OrdersPage`
  - `PaymentResultPage`
  - `AccountPage`
- Admin pages:
  - `AdminProductsPage`
  - `AdminCategoriesPage`
  - `AdminPaymentsPage`
  - `AdminPermissionsPage`
  - `AdminOrdersPendingPage`
  - `AdminLayout`
  - `AdminForbiddenPage`
- API modules:
  - `authApi`
  - `catalogApi`
  - `cartApi`
  - `orderApi`
  - `paymentApi`
  - `userApi`
  - `adminApi`
- Backend Postman collection exists under `postman/`.

Main gaps:

- Storefront home endpoint is not fully wired as homepage source of truth.
- Product filters need to expose book metadata: author, publisher, ISBN, format.
- Product detail needs reviews/rating flow.
- Checkout needs stronger preview/coupon/payment UX.
- Admin area is missing full pages for dashboard, all orders lifecycle, users, coupons, promotions, reviews, refunds.
- Admin pending orders page should become full order management.
- API layer is missing many backend endpoints from Phase 2-9.
- No frontend test setup yet.
- No CI/build quality gate for frontend.
- No production deployment docs for frontend.

## Architecture Rules

- Keep frontend inside `frontend/`.
- Keep API calls inside `src/api/*.js`; components should not call `fetch` directly.
- Keep route-level pages in `src/pages`.
- Keep reusable UI pieces in `src/components`.
- Add feature-specific components under clear folders when needed:
  - `src/components/catalog`
  - `src/components/cart`
  - `src/components/checkout`
  - `src/components/reviews`
  - `src/components/admin`
- Keep DTO mapping/normalization in `src/utils/mappers.js` or feature helpers.
- Do not duplicate backend business rules beyond UX hints. Always display backend validation errors.
- Use `ApiResponse.data` via existing `request()` wrapper.
- Use route guards for admin and authenticated customer pages.
- Keep UI dense and operational for admin; avoid marketing-style admin screens.
- Keep customer storefront polished but practical for bookstore shopping.

## Phase FE-1: API Layer Completion

Status: completed in current workspace. `npm run build` passes after installing frontend dependencies with `npm ci`.

Goal: expose every backend capability through consistent frontend API modules before building screens.

Add/update API modules:

- [x] `src/api/storefrontApi.js`
  - [x] `getStorefrontHome()`
- [x] `src/api/reviewApi.js`
  - [x] `getProductReviews(slug, params)`
  - [x] `createOrderItemReview(orderId, orderItemId, body)`
  - [x] `updateReview(reviewId, body)`
  - [x] `deleteReview(reviewId)`
- [x] `src/api/checkoutApi.js`
  - [x] `previewCheckout(body)`
  - [x] `createCheckout(body)`
- [x] `src/api/adminProductsApi.js`
  - [x] Product, media, variation, and stock endpoints.
- [x] `src/api/adminCategoriesApi.js`
  - [x] Admin category mutation endpoints.
- [x] `src/api/adminOrdersApi.js`
  - [x] `getAdminOrders(params)`
  - [x] `getAdminOrder(orderId)`
  - [x] `confirmOrder(orderId)`
  - [x] `markPacking(orderId)`
  - [x] `markShipping(orderId)`
  - [x] `markCompleted(orderId)`
  - [x] `cancelAdminOrder(orderId, body)`
  - [x] `markRefunded(orderId, body)`
- [x] `src/api/adminUsersApi.js`
  - [x] `getAdminUsers(params)`
  - [x] `getAdminUser(userId)`
  - [x] `lockUser(userId)`
  - [x] `unlockUser(userId)`
  - [x] `updateUserRoles(userId, roles)`
- [x] `src/api/adminPermissionsApi.js`
  - [x] Roles, permissions, role-permission, and user-permission endpoints.
- [x] `src/api/adminCouponsApi.js`
  - [x] CRUD coupon endpoints.
- [x] `src/api/adminPromotionsApi.js`
  - [x] CRUD promotion endpoints.
- [x] `src/api/adminReviewsApi.js`
  - [x] `getAdminReviews(params)`
  - [x] `moderateReview(reviewId, body)`
  - [x] `replyReview(reviewId, body)`
- [x] `src/api/adminDashboardApi.js`
  - [x] Summary, sales, orders, top-books, low-stock.
- [x] `src/api/adminPaymentsApi.js`
  - [x] Payment group reconciliation endpoint.
- [x] Backward compatibility re-exports
  - [x] `adminApi.js` re-exports split admin modules.
  - [x] `orderApi.js` keeps legacy checkout/address exports.

Improve `client.js`:

- [x] Keep bearer token injection.
- [x] Keep `credentials: "include"`.
- [x] Add `skipAuth` support for public/no-auth requests.
- [x] Add optional request abort support for search/filter screens through `options.signal`.
- [x] Normalize error object:
  - [x] `message`
  - [x] `status`
  - [x] `errorCode`
  - [x] `payload`
  - [x] `data`
- [x] `query(params)` drops `undefined`, `null`, and `""`, but keeps `false` and `0`.
- [ ] Add helper for paginated response shape where useful.

Acceptance:

- [x] All backend Phase 1-9 endpoints used by FE have a centralized API function.
- [x] No page directly constructs raw backend API URLs except through API modules.
- [x] Errors from backend are visible to UI with normalized metadata.
- [x] `npm run build` passes.

## Phase FE-2: Auth, Session, And Route Guards

Status: completed in current workspace. `npm run build` passes.

Goal: make customer/admin access reliable and predictable.

Tasks:

- [x] Audit current `AuthModal` against backend auth payload.
- [x] Ensure login saves:
  - [x] access token
  - [x] refresh token if body output enabled
  - [x] current user from `/users/me` after login when available
  - [x] roles through saved profile/JWT claims
- [x] Refresh token behavior:
  - [x] On 401, attempt one refresh if refresh token/cookie exists.
  - [x] Share one refresh request across concurrent 401 responses.
  - [x] Retry the original request once with the new access token.
  - [x] If refresh fails, clear auth and redirect/open login.
- [x] Add explicit flows:
  - [x] register
  - [x] verify email OTP
  - [x] resend verification OTP
  - [x] forgot password
  - [x] reset password
  - [x] logout
  - [x] logout all
  - [x] active sessions list/revoke
- [x] Admin guard:
  - [x] Use role `ADMIN` and/or effective permissions/JWT claims.
  - [x] Redirect unauthorized user to `/admin/forbidden`.
  - [x] Redirect guest admin access to login with `next`.
- [x] Customer guard:
  - [x] Cart/checkout/orders/account routes require login.
  - [x] Product action-level auth still prompts login.
- [x] Storage/session helpers:
  - [x] `hasAccessToken()`
  - [x] `getAuthSnapshot()`
  - [x] `saveAccessToken(token)`
  - [x] `saveRefreshToken(refreshToken)`
  - [x] `aivira-auth-expired` event.

Acceptance:

- [x] User can register, verify email, login, refresh session, logout.
- [x] Admin route cannot be accessed by normal user.
- [x] Expired token produces clean UX, not broken screens.
- [x] `npm run build` passes after FE-2 changes.

## Phase FE-3: Public Storefront Home

Goal: homepage uses backend `GET /storefront/home` and becomes real bookstore storefront.

Sections:

- Featured books.
- New arrivals.
- Bestselling books.
- Category highlights.
- Optional editorial/static sections only after live data sections.

Tasks:

- Wire `HomePage` to `storefrontApi.getStorefrontHome()`.
- Use skeleton/loading states.
- Empty states:
  - No featured books.
  - No bestselling books.
  - No categories.
- Book card should display:
  - cover/thumbnail
  - title
  - author
  - price/original price
  - stock state
  - quick add or detail link
- Category highlights should link to `/category/:slug`.
- Preserve current visual polish/animation but do not let animation block usability.

Acceptance:

- Fresh seeded backend can render useful homepage immediately.
- No hardcoded book list is required for normal homepage content.

## Phase FE-4: Catalog Search, Filter, And Listing

Goal: make `/category/:slug` or catalog listing useful for bookstore browsing.

Filters:

- Keyword.
- Category.
- Author.
- Publisher.
- ISBN.
- Price min/max.
- Availability.
- Sort:
  - newest
  - price_asc
  - price_desc
  - best_selling
  - name_asc

Tasks:

- Expand `catalogApi.getProducts(params)`.
- Add filter panel suitable for desktop and mobile.
- Keep current category route working.
- Add "All books" route if useful:
  - `/books`
  - or keep `/category/all`.
- Persist filters in URL query params.
- Add pagination controls.
- Add loading/empty/error states.
- Make filter labels bookstore-specific.

Acceptance:

- Public user can browse and search by title/author/publisher/ISBN.
- URL can be shared and restores filters.
- Mobile filtering is usable.

## Phase FE-5: Product Detail, Reviews, And Add-To-Cart

Goal: product detail should feel complete for a book.

Product detail content:

- Cover gallery.
- Title.
- Author.
- ISBN.
- Publisher.
- Publication year.
- Language.
- Page count.
- Format.
- Dimensions.
- Description.
- Category.
- Price/discount.
- Stock.
- Variation selector.
- Quantity stepper.
- Add to cart.

Reviews:

- Public approved reviews list.
- Filter by rating.
- Sort newest/oldest/rating.
- Admin reply display.
- Customer review CTA when applicable.

Tasks:

- Extend `ProductPage` with book metadata.
- Load reviews from `/products/{slug}/reviews`.
- Add review components:
  - `ReviewList`
  - `ReviewCard`
  - `RatingStars`
  - `ReviewForm`
- Create/update/delete review flows from order detail or order item actions.
- Handle backend errors:
  - order not completed
  - duplicate review
  - review not allowed

Acceptance:

- Public product detail includes book metadata and reviews.
- Customer can add selected variation to cart.
- Customer can review completed purchased item.

## Phase FE-6: Cart And Checkout

Goal: checkout mirrors backend discount/payment behavior.

Cart:

- List items with cover/title/author/variation/price/quantity/subtotal.
- Update quantity.
- Remove item.
- Select items for checkout if backend supports selected cart item ids.
- Stock error handling.

Checkout:

- Address selection and create/edit address shortcut.
- Coupon input.
- Preview before submit using `POST /checkout/preview`.
- Display:
  - subtotal
  - promotion discount
  - coupon discount
  - shipping fee
  - total
  - applied promotions
  - item final line totals
- Payment methods:
  - COD
  - VNPay if enabled/returned by backend/config
  - MoMo if enabled/returned by backend/config
- Submit checkout.
- Redirect/open provider payment URL for online flows if backend returns one.
- Payment result page handles VNPay/MoMo return.

Tasks:

- Ensure `CheckoutPage` calls preview whenever address/cart/coupon/payment changes.
- Debounce coupon preview.
- Clear cart after successful checkout if backend did.
- Preserve order/payment group code for result page.

Acceptance:

- Preview and final checkout totals match.
- Invalid coupon shows backend message.
- COD checkout creates order and shows order detail.
- Online checkout directs user to payment or result state.

## Phase FE-7: Customer Account, Addresses, Orders, Payments

Goal: customer self-service is complete.

Account:

- View/update profile.
- Change password.
- Avatar upload if backend supports current endpoint.
- Deactivate account.
- Active sessions list and revoke.

Addresses:

- List/create/update/delete.
- Set default.
- Inline use during checkout.

Orders:

- List with status/payment status filters.
- Detail view:
  - item snapshots
  - totals
  - payment status
  - shipping address
  - refund metadata if present
- Cancel allowed early orders.
- Payment retry for retryable online payment group.
- Review actions for completed order items.

Acceptance:

- Customer can manage profile/address/order lifecycle without admin support.
- Payment retry and cancellation errors are clearly shown.

## Phase FE-8: Admin Layout And Dashboard

Goal: admin starts with a practical bookstore operations dashboard.

Admin navigation:

- Dashboard.
- Books.
- Categories.
- Orders.
- Payments.
- Coupons.
- Promotions.
- Reviews.
- Users.
- Roles/Permissions.
- Settings/docs links if needed.

Dashboard widgets:

- Revenue.
- Order count.
- Successful/failed payments.
- New users.
- Pending orders.
- Pending payments.
- Low-stock count.
- Sales points table/chart.
- Order status counts.
- Top books.
- Low-stock books.

Tasks:

- Add `AdminDashboardPage`.
- Wire `/admin/dashboard`.
- Make `/admin` redirect to dashboard, not products.
- Add date range filters.
- Add limit/threshold controls for top/low-stock.
- Use compact admin UI with tables and summary tiles.

Acceptance:

- Admin can understand store health from one screen.
- Dashboard uses backend live data.

## Phase FE-9: Admin Catalog Management

Goal: admin book/category management is complete and ergonomic.

Books:

- List/search/filter admin products.
- Create book.
- Edit book.
- Soft delete book.
- Upload/update/delete media.
- Manage variations.
- Update stock.
- Validate required fields:
  - productName
  - sku
  - bookAuthor
  - categoryId
  - price
  - at least one variation
- Book metadata fields:
  - author
  - ISBN
  - publisher
  - publication year
  - language
  - page count
  - format
  - dimensions
- Duplicate ISBN/SKU/slug backend errors displayed.

Categories:

- Tree/list view.
- Create/update/delete category.
- Parent category selector.
- Active/visible flags.
- Display order.

Acceptance:

- Admin can manage all bookstore catalog data without Postman.
- New book created from FE is immediately visible in public catalog.

## Phase FE-10: Admin Order Lifecycle And Manual Refund

Goal: replace pending-only orders page with full admin order operations.

Pages:

- `AdminOrdersPage`
- `AdminOrderDetailPage` or drawer/modal detail.

List filters:

- status.
- keyword.
- fromDate.
- toDate.
- page/size.

Actions:

- Confirm.
- Mark packing.
- Mark shipping.
- Mark completed.
- Cancel.
- Mark refunded.

UX rules:

- Only show valid next actions based on current status.
- Paid cancellation should guide admin to manual refund.
- Manual refund form requires amount, reason, note.
- Show stock restore/coupon/refund behavior notes where useful.
- Show payment group/payment status.

Acceptance:

- Admin can run COD order lifecycle end to end.
- Invalid transitions are not offered in UI and backend errors are still handled.
- Paid pre-shipping order can be marked refunded.

## Phase FE-11: Admin Users, Roles, And Permissions

Goal: expose practical user/RBAC management.

Users:

- List/filter by keyword, role, active, locked, emailVerified.
- Detail view with roles and account flags.
- Lock/unlock user.
- Replace roles with USER/ADMIN.
- Prevent self-lock/self-role-change in UI when current user id matches.

Permissions:

- Existing role permissions page should be verified.
- Direct user permissions:
  - list effective/direct permissions.
  - grant permission with optional reason/expiresAt.
  - revoke permission.

Acceptance:

- Admin can inspect and change access state safely.
- No seller/shop roles are shown.

## Phase FE-12: Admin Coupons And Promotions

Goal: admin can manage discounts without backend/Postman.

Coupons:

- List/detail.
- Create/update/deactivate.
- Fields:
  - code
  - type PERCENT/FIXED
  - value
  - maxDiscountAmount
  - minOrderAmount
  - usageLimit
  - usageLimitPerUser
  - startAt/endAt
  - active
  - usedCount

Promotions:

- List/detail.
- Create/update/deactivate.
- Fields:
  - promotionName
  - description
  - promotionType
  - value
  - maxDiscountAmount
  - promotionScope PRODUCT/CATEGORY
  - targetId with product/category picker
  - startAt/endAt
  - active

Validation:

- Date range.
- Percent/fixed positive values.
- Required target.
- Backend duplicate name/code errors.

Acceptance:

- Admin can create coupon/promotion and immediately test it in checkout preview.

## Phase FE-13: Admin Review Moderation

Goal: admin can moderate bookstore reviews.

List filters:

- approved.
- visible.
- rating.
- keyword.
- productId.
- userId.
- page/size.

Actions:

- Approve/unapprove.
- Hide/show.
- Reply.
- Clear reply.

Review display:

- Rating.
- Comment.
- Images.
- Product/book info.
- Customer info.
- Order/order item id.
- Moderation metadata.

Acceptance:

- Public only sees approved visible reviews.
- Admin reply appears on public product detail.

## Phase FE-14: Admin Payments And Reconciliation

Goal: admin payment screen is operationally useful.

Tasks:

- Audit existing `AdminPaymentsPage`.
- Show payment group detail.
- Show payment status, method, provider transaction ids, amount, paidAt.
- Reconcile payment group.
- Link payment group to order detail.
- Make provider callback/IPN pages not customer-facing except result page.

Acceptance:

- Admin can investigate pending/failed payments and run reconcile.

## Phase FE-15: UI System, Accessibility, And Responsiveness

Goal: make the app feel finished, not stitched together.

Build shared UI components:

- Button.
- IconButton.
- Input.
- Select.
- Textarea.
- Checkbox/toggle.
- Date/time input.
- Modal/dialog.
- Drawer.
- Table.
- Pagination.
- Tabs.
- Toast/notification.
- Empty state.
- Error state.
- Skeleton.
- Badge/status pill.
- Confirm dialog.

Accessibility:

- Keyboard focus visible.
- Modals trap focus.
- Buttons have labels/tooltips.
- Form errors connected to fields.
- Color contrast acceptable.
- Images have alt text.

Responsive:

- Mobile catalog filters as drawer.
- Cart/checkout readable on mobile.
- Admin tables use horizontal scroll or responsive columns.
- No text overlap in buttons/cards.

Acceptance:

- Common UI is consistent across customer/admin.
- Main flows work on mobile and desktop.

## Phase FE-16: Internationalization And Copy

Goal: keep Vietnamese/English support maintainable.

Tasks:

- Audit `src/i18n.js`; split into namespace files if it becomes too large.
- Add keys for all new pages:
  - admin dashboard.
  - orders/refunds.
  - users.
  - coupons/promotions.
  - reviews.
  - checkout preview.
- Remove hardcoded user-facing strings from components.
- Keep technical admin labels precise.

Acceptance:

- Language switch works across all new screens.
- No visible raw translation keys.

## Phase FE-17: Frontend Testing

Goal: add safety net for critical flows.

Add dependencies:

- Vitest.
- React Testing Library.
- MSW for API mocking.
- Playwright for E2E if feasible.

Unit/component tests:

- API client error handling.
- Auth modal login success/error.
- Product filter query building.
- Checkout preview totals rendering.
- Admin order action visibility.
- Coupon/promotion forms.
- Review form validation.

E2E smoke tests:

- Public browse product detail.
- Login.
- Add to cart.
- Checkout preview.
- Admin dashboard loads.
- Admin product create/edit smoke.

Acceptance:

- `npm test` or `npm run test` available.
- `npm run build` passes.
- Core user/admin flows have smoke coverage.

## Phase FE-18: Frontend Config, Build, And Deployment

Goal: production-practical frontend delivery.

Config:

- `.env.example` documents:
  - `VITE_API_BASE_URL`
  - optional feature flags if introduced.
- Local dev uses Vite proxy to `/api/v1`.
- Production points to deployed backend API.

Build:

- `npm run build`.
- `npm run preview`.
- Add lint/format scripts if dependencies are added:
  - ESLint.
  - Prettier.

Deployment docs:

- Static hosting option.
- Reverse proxy option.
- CORS requirements.
- Refresh cookie secure/sameSite considerations.
- Payment return URL configuration.

Acceptance:

- A developer can run and build FE using README instructions.
- Production environment variables are clearly documented.

## Recommended Implementation Order

1. Complete API modules for backend Phase 1-9.
2. Stabilize auth/session/route guards.
3. Wire storefront home and catalog filters.
4. Finish product detail and reviews.
5. Finish cart/checkout/payment result.
6. Finish customer account/orders/review actions.
7. Build admin dashboard.
8. Complete admin order lifecycle/refund.
9. Complete admin users/RBAC.
10. Complete admin coupons/promotions.
11. Complete admin review moderation.
12. Polish admin payments/reconciliation.
13. Extract shared UI components and fix responsive/accessibility issues.
14. Complete i18n copy.
15. Add tests and CI/build docs.

## Route Target Map

Public/customer:

- `/`
- `/category/all`
- `/category/:slug`
- `/product/:slug`
- `/cart`
- `/checkout`
- `/orders`
- `/orders/:orderId` if detail page is split out.
- `/account`
- `/payment-result`
- `/login` and `/register` redirect/open auth modal.

Admin:

- `/admin/dashboard`
- `/admin/products`
- `/admin/categories`
- `/admin/orders`
- `/admin/orders/:orderId` if detail page is split out.
- `/admin/payments`
- `/admin/users`
- `/admin/users/:userId`
- `/admin/coupons`
- `/admin/promotions`
- `/admin/reviews`
- `/admin/permissions`
- `/admin/forbidden`

## API Coverage Checklist

Public:

- [ ] `GET /storefront/home`
- [ ] `GET /products`
- [ ] `GET /products/{slug}`
- [ ] `GET /products/{slug}/reviews`
- [ ] `GET /categories`
- [ ] `GET /categories/tree`

Customer:

- [ ] Auth register/verify/resend/login/refresh/logout/logout-all/sessions.
- [ ] Profile update/avatar/password/deactivate.
- [ ] Addresses CRUD/default.
- [ ] Cart get/add/update/remove/clear.
- [ ] Checkout preview/create.
- [ ] Orders list/detail/cancel.
- [ ] Payments group/detail/retry/result.
- [ ] Reviews create/update/delete.

Admin:

- [ ] Products/media/variations/stock.
- [ ] Categories.
- [ ] Orders lifecycle/refund.
- [ ] Users lock/unlock/roles.
- [ ] Roles/permissions/direct permissions.
- [ ] Coupons.
- [ ] Promotions.
- [ ] Reviews moderation/reply.
- [ ] Dashboard.
- [ ] Payments reconciliation.

## Acceptance Criteria For Complete Frontend

The frontend is complete when:

- Public users can browse, search, filter, and view book details.
- Public users can read approved reviews.
- Customers can register, verify email, login, manage profile/addresses/sessions.
- Customers can manage cart, preview checkout, apply coupon, checkout, pay, retry payment, and track orders.
- Customers can review completed purchased order items.
- Admin can manage books, categories, media, variations, stock, orders, refunds, users, permissions, coupons, promotions, reviews, payments, and dashboard.
- All major backend errors are displayed clearly.
- No seller/shop/merchant UI exists.
- UI works on mobile and desktop.
- `npm run build` passes.
- Frontend README/env docs are accurate.
- Critical flows have automated tests or at least Playwright smoke coverage.

## Explicit Non-Goals

- No seller/shop/merchant dashboard.
- No provider refund integration UI beyond manual refund marking.
- No CMS/banner builder yet.
- No advanced analytics/export yet.
- No separate author management table/page yet.
- No real-time notifications yet.
