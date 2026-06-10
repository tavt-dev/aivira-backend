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

Status: completed in current workspace. `npm run build` passes.

Goal: homepage uses backend `GET /storefront/home` and becomes real bookstore storefront.

Sections:

- [x] Featured books.
- [x] New arrivals.
- [x] Bestselling books.
- [x] Category highlights.
- [x] Optional editorial/static sections only after live data sections.

Tasks:

- [x] Wire `HomePage` to `storefrontApi.getStorefrontHome()`.
- [x] Use abort signal for storefront request cleanup.
- [x] Normalize `featuredBooks`, `newArrivals`, `bestsellingBooks`, and `categoryHighlights`.
- [x] Use skeleton/loading states.
- [x] Empty states:
  - [x] No featured books.
  - [x] No new arrivals.
  - [x] No bestselling books.
  - [x] No categories.
- [x] Book card should display:
  - [x] cover/thumbnail
  - [x] title
  - [x] author
  - [x] price/original price
  - [x] stock state
  - [x] detail link
- [x] Category highlights should link to `/category/:slug`.
- [x] Preserve current visual polish/animation but do not let animation block usability.
- [x] Add mobile hero book stack because desktop orbit is hidden on mobile.
- [x] Add i18n keys for storefront loading/error/empty/live sections.

Acceptance:

- [x] Fresh seeded backend can render useful homepage immediately.
- [x] No hardcoded book list is required for normal homepage content.
- [x] `npm run build` passes.

## Phase FE-4: Catalog Search, Filter, And Listing

Status: completed in current workspace. `npm run build` passes.

Goal: make `/category/:slug` or catalog listing useful for bookstore browsing.

Filters:

- [x] Keyword.
- [x] Category.
- [x] Author.
- [x] Publisher.
- [x] ISBN.
- [x] Price min/max.
- [x] Availability.
- [x] Sort:
  - [x] newest
  - [x] price_asc
  - [x] price_desc
  - [x] best_selling
  - [x] name_asc

Tasks:

- [x] Expand `catalogApi.getProducts(params)`.
- [x] Add filter panel suitable for desktop and mobile.
- [x] Keep current category route working.
- [x] Keep `/category/all` as the all-books route.
- [x] Persist filters in URL query params.
- [x] Add pagination controls.
- [x] Add loading/empty/error states.
- [x] Make filter labels bookstore-specific.
- [x] Preserve legacy navbar `search` query by mapping it to backend `keyword`.
- [x] Move filtering/sorting/pagination to backend `GET /products`.

Acceptance:

- [x] Public user can browse and search by title/author/publisher/ISBN.
- [x] URL can be shared and restores filters.
- [x] Mobile filtering is usable.
- [x] `npm run build` passes after FE-4 changes.

## Phase FE-5: Product Detail, Reviews, And Add-To-Cart

Status: completed in current workspace. `npm run build` passes.

Goal: product detail should feel complete for a book.

Product detail content:

- [x] Cover gallery.
- [x] Title.
- [x] Author.
- [x] ISBN.
- [x] Publisher.
- [x] Publication year.
- [x] Language.
- [x] Page count.
- [x] Format.
- [x] Dimensions.
- [x] Description.
- [x] Category.
- [x] Price/discount.
- [x] Stock.
- [x] Variation selector.
- [x] Quantity stepper.
- [x] Add to cart.

Reviews:

- [x] Public approved reviews list.
- [x] Filter by rating.
- [x] Sort newest/oldest/rating.
- [x] Admin reply display.
- [x] Customer review CTA when applicable.

Tasks:

- [x] Extend `ProductPage` with book metadata.
- [x] Load reviews from `/products/{slug}/reviews`.
- [x] Add review components:
  - [x] `ReviewList`
  - [x] `ReviewCard`
  - [x] `RatingStars`
  - [x] `ReviewForm`
- [x] Create review flow from completed order item actions.
- [x] Keep update/delete review API support available for later UI.
- [x] Handle backend review errors through inline backend messages:
  - [x] order not completed
  - [x] duplicate review
  - [x] review not allowed
- [x] Add i18n keys for product metadata, variations, reviews, and order review CTA.

Acceptance:

- [x] Public product detail includes book metadata and reviews.
- [x] Customer can add selected variation to cart.
- [x] Customer can review completed purchased item.
- [x] `npm run build` passes.

## Phase FE-6: Cart And Checkout

Status: completed in current workspace. `npm run build` passes.

Goal: checkout mirrors backend discount/payment behavior.

Cart:

- [x] List items with cover/title/author/variation/price/quantity/subtotal.
- [x] Update quantity with stepper controls.
- [x] Remove item.
- [x] Select items for checkout using backend cart item ids.
- [x] Persist selected checkout ids in session storage.
- [x] Stock and availability error handling.

Checkout:

- [x] Address selection and create-address shortcut.
- [x] Coupon input.
- [x] Preview before submit using `POST /checkout/preview`.
- [x] Display:
  - [x] subtotal
  - [x] promotion discount
  - [x] coupon discount
  - [x] shipping fee
  - [x] total
  - [x] applied promotions
  - [x] item final line totals
- [x] Payment methods:
  - [x] COD
  - [x] VNPay
  - [x] MoMo
- [x] Submit checkout through `POST /checkout`.
- [x] Redirect/open provider payment URL for online flows if backend returns one.
- [x] Show QR fallback if backend only returns `qrCodeUrl`.
- [x] Payment result page remains compatible with VNPay/MoMo return.

Tasks:

- [x] Ensure `CheckoutPage` calls preview whenever address/cart/coupon/payment changes.
- [x] Debounce coupon preview.
- [x] Clear selected checkout ids and refresh cart badge after successful checkout.
- [x] Preserve order/payment group code in checkout success state.
- [x] Add checkout selection helper.
- [x] Add i18n keys for cart selection, coupon, preview totals, payment redirect, and QR fallback.

Acceptance:

- [x] Preview and final checkout totals come from the same backend discount rules.
- [x] Invalid coupon shows backend message.
- [x] COD checkout creates order and links to orders.
- [x] Online checkout directs user to payment URL or QR result state.
- [x] `npm run build` passes.

## Phase FE-7: Customer Account, Addresses, Orders, Payments

Status: completed in current workspace. `npm run build` passes.

Goal: make customer self-service complete and production-practical after cart/checkout is live.

Summary:

- Finish `AccountPage` as the customer profile/address/session hub.
- Upgrade `OrdersPage` from a simple list/modal into a usable order tracking screen.
- Add payment retry UX for failed/expired/retryable online payment groups.
- Keep review creation for completed order items from FE-5, but prevent duplicate UX actions where possible.
- Do not add seller/shop/merchant UI.

Account:

- [x] Keep profile view/update wired to `/users/me`.
- [x] Show immutable identity fields clearly:
  - [x] username
  - [x] email
  - [x] provider
  - [x] email verified state
- [x] Profile editable fields:
  - [x] firstName
  - [x] lastName
  - [x] gender
  - [x] phoneNumber displayed read-only because the current update DTO does not accept it.
- [x] Avatar upload:
  - [x] keep `updateAvatar(file)`.
  - [x] show upload busy state.
  - [x] preview selected image only after backend success.
  - [x] show backend validation error for invalid file.
- [x] Change password:
  - [x] require current password.
  - [x] require new password and confirm password match.
  - [x] clear password form after success.
  - [x] show backend message for invalid current password.
- [x] Account deactivate:
  - [x] add explicit danger-zone section.
  - [x] require confirm dialog or typed confirmation.
  - [x] call `deactivateAccount()`.
  - [x] clear auth and redirect/open login after success.
- [x] Sessions:
  - [x] list active sessions from backend.
  - [x] show current session badge.
  - [x] revoke non-current session and remove it from UI.
  - [x] revoke current session clears local auth and opens login.
  - [x] logout all clears auth and opens login.

Addresses:

- [x] Keep address APIs from `userApi.js`, not legacy `orderApi.js`.
- [x] List addresses with recipient, phone, full address, default badge.
- [x] Create address.
- [x] Update address.
- [x] Delete address with confirm dialog.
- [x] Set default address.
- [x] Reset address form after save/cancel.
- [x] Add address form validation before API call:
  - [x] recipientName required.
  - [x] phoneNumber required.
  - [x] addressLine required.
  - [x] city required if backend requires it.
- [x] Keep checkout address shortcut behavior compatible with FE-6.

Orders List:

- [x] Replace plain order list with filterable order dashboard.
- [x] Query backend `getOrders(params)` using URL search params.
- [x] Filters:
  - [x] order status.
  - [x] hide payment status filter because current customer endpoint does not support it.
  - [x] hide keyword/order code filter because current customer endpoint does not support it.
  - [x] page.
  - [x] size.
- [x] If backend customer list does not support a filter yet, keep the control hidden instead of filtering client-side.
- [x] Use `PageResponse` pagination:
  - [x] currentPage.
  - [x] totalPages.
  - [x] totalElements.
  - [x] hasNext.
  - [x] hasPrevious.
- [x] Order cards/table show:
  - [x] orderCode.
  - [x] orderStatus.
  - [x] paymentMethod.
  - [x] paymentStatus.
  - [x] totalAmount.
  - [x] createdAt.
  - [x] itemCount.
- [x] Add status badges with customer-friendly labels.
- [x] Add loading skeleton, empty state, and backend error state.

Order Detail:

- [x] Keep detail loaded by `getOrder(orderId)` when opening a modal/drawer.
- [x] Consider route detail `/orders/:orderId` only if needed later; FE-7 can keep modal/drawer.
- [x] Detail displays:
  - [x] orderCode.
  - [x] orderStatus.
  - [x] paymentMethod.
  - [x] paymentStatus.
  - [x] paymentGroupCode.
  - [x] shipping recipient/phone/address.
  - [x] item snapshots: book name, thumbnail if available, variation/SKU, quantity, unit price, discount, final line total.
  - [x] subtotal, discountAmount, shippingFee, totalAmount.
  - [x] couponCode if present in backend response.
  - [x] refund metadata if `refund` exists.
  - [x] createdAt/updatedAt if present.
- [x] Detail action area:
  - [x] cancel button only for statuses customer can cancel.
  - [x] retry payment button only when online payment is retryable.
  - [x] write review button only when order is `COMPLETED`.

Cancel Order:

- [x] Use existing `cancelOrder(id, reason)`.
- [x] Show cancel action only for safe customer-cancellable statuses:
  - [x] `PENDING_CONFIRMATION`.
  - [x] unpaid/retryable pre-fulfillment statuses if backend allows.
- [x] Ask for cancel reason in a small modal/input.
- [x] On success:
  - [x] update list item.
  - [x] update open detail.
  - [x] show success message.
- [x] On failure:
  - [x] show backend `error.message`, especially paid-order/refund-required cases.

Payment Retry:

- [x] Use `retryPayment(paymentGroupCode)` from `paymentApi.js`.
- [x] Show retry only when:
  - [x] payment method is `VNPAY` or `MOMO`.
  - [x] payment status is retryable: FAILED, CANCELLED, or EXPIRED.
  - [x] `paymentGroupCode` exists.
- [x] On retry success:
  - [x] if response has `paymentUrl || deeplink`, redirect after short delay and show fallback button.
  - [x] if response has `qrCodeUrl`, show QR fallback.
  - [x] preserve current order detail state.
- [x] On retry failure:
  - [x] show backend error message.
  - [x] keep user on orders page.
- [x] Payment result page remains the return landing page for provider callbacks.

Review Actions:

- [x] Keep FE-5 review creation flow from completed order item.
- [x] Hide review CTA for non-completed orders.
- [x] After successful review creation:
  - [x] close form.
  - [x] show pending moderation message.
  - [x] disable the CTA locally for that order item to avoid repeated submit attempts.
- [x] Duplicate review backend error remains visible if backend rejects it.
- [x] Do not add review edit/delete UI in FE-7 unless current user-owned reviews are available in an order-safe response.

State, UX, And i18n:

- [x] Add dedicated order/account loading states instead of silent blank panels.
- [x] Replace raw status strings with compact labels where possible, but preserve raw backend status in fallback.
- [x] Add confirm modal component locally if shared UI phase is not started yet.
- [x] Add i18n EN/VI keys for:
  - [x] order filters.
  - [x] order status labels.
  - [x] payment retry.
  - [x] cancel reason.
  - [x] refund metadata.
  - [x] account danger zone/deactivate.
  - [x] session revoke/logout all states.
- [x] Keep mobile layout usable:
  - [x] account panels stack cleanly.
  - [x] orders list becomes card layout.
  - [x] detail modal/drawer scrolls inside viewport.

Tasks:

- [x] Refactor `OrdersPage` into smaller local components or `components/orders`:
  - [x] `OrderFilters`.
  - [x] `OrderCard` or `OrderTable`.
  - [x] `OrderDetailModal`.
  - [x] `CancelOrderModal`.
  - [x] inline payment retry panel inside order detail.
- [x] Tighten `AccountPage`:
  - [x] add deactivate flow.
  - [x] add busy states.
  - [x] add address cancel/reset.
  - [x] improve sessions section.
- [x] Extend mappers if needed:
  - [x] `normalizeOrder`.
  - [x] `normalizePaymentGroup`.
  - [x] optional `normalizeAddress`.
- [x] Keep API calls centralized in:
  - [x] `orderApi.js`.
  - [x] `paymentApi.js`.
  - [x] `userApi.js`.
- [x] Update this plan with `[x]` after implementation.

Test plan:

- [x] Run `cd frontend && npm run build`.
- [ ] Manual smoke checklist:
  - [ ] account profile loads and saves.
  - [ ] avatar upload success/error displays correctly.
  - [ ] password change validates confirm password.
  - [ ] address create/update/delete/default works.
  - [ ] current session revoke clears auth.
  - [ ] logout all clears auth.
  - [ ] orders list loads with pagination.
  - [ ] order detail shows shipping, items, totals, payment, refund metadata.
  - [ ] cancel allowed order updates UI.
  - [ ] invalid cancel shows backend error.
  - [ ] online retry redirects or shows QR fallback.
  - [ ] completed order item can open review form.
  - [ ] non-completed order item does not show review CTA.
  - [ ] mobile account/orders screens do not overlap.

Acceptance:

- [x] Customer can manage profile, avatar, password, sessions, and account deactivation without admin support.
- [x] Customer can manage addresses fully and those addresses stay compatible with checkout.
- [x] Customer can list, filter/page, inspect, cancel eligible orders, retry eligible payments, and review completed items.
- [x] Payment retry and cancellation errors are shown from backend messages.
- [x] Refund metadata is visible when backend returns it.
- [x] No seller/shop/merchant UI exists.
- [x] `npm run build` passes.

## Phase FE-8: Admin Layout And Dashboard

Status: completed in current workspace. `npm run build` passes.

Goal: admin starts with a practical bookstore operations dashboard.

Admin navigation:

- [x] Dashboard.
- [x] Books.
- [x] Categories.
- [x] Orders pending placeholder.
- [x] Payments.
- [x] Roles/Permissions.
- [x] Keep users/coupons/promotions/reviews routes out until their pages are implemented.

Dashboard widgets:

- [x] Revenue.
- [x] Order count.
- [x] Successful/failed payments.
- [x] New users.
- [x] Pending orders.
- [x] Pending payments.
- [x] Low-stock count.
- [x] Sales points table/bar list.
- [x] Order status counts.
- [x] Top books.
- [x] Low-stock books.

Tasks:

- [x] Add `AdminDashboardPage`.
- [x] Wire `/admin/dashboard`.
- [x] Make `/admin` redirect to dashboard, not products.
- [x] Make `/admin/login` use `next=/admin/dashboard`.
- [x] Add date range filters.
- [x] Add limit/threshold controls for top/low-stock.
- [x] Fetch dashboard endpoints in parallel.
- [x] Use per-section error messages.
- [x] Use compact admin UI with tables and summary tiles.
- [x] Add EN/VI i18n keys for dashboard.

Acceptance:

- [x] Admin can understand store health from one screen.
- [x] Dashboard uses backend live data.
- [x] Existing admin routes remain wired.
- [x] No seller/shop/merchant navigation is introduced.
- [x] `npm run build` passes.
- [ ] Manual browser smoke checklist remains to run against a live backend.

## Phase FE-9: Admin Catalog Management

Goal: admin book/category management is complete and ergonomic.

Books:

- [x] List/search/filter admin products using backend `status`, `categoryId`, `keyword`, `page`, and `size`.
- [x] Render dense admin book list with cover, title, author, ISBN, category, price, stock, status, featured state, and timestamps.
- [x] Create book with bookstore metadata and default variation.
- [x] Edit book metadata, pricing, category, and featured flag.
- [x] Soft delete book with confirmation.
- [x] Upload/update/delete media.
- [x] Manage variations with create/edit/delete.
- [x] Update variation stock.
- [x] Validate required fields:
  - productName
  - sku
  - bookAuthor
  - categoryId
  - price
  - at least one variation
- [x] Book metadata fields:
  - author
  - ISBN
  - publisher
  - publication year
  - language
  - page count
  - format
  - dimensions
- [x] Duplicate ISBN/SKU/slug backend errors displayed through backend `error.message`.

Categories:

- [x] Tree/list view.
- [x] Create/update/delete category.
- [x] Parent category selector.
- [x] Active/visible flags.
- [x] Display order.

Acceptance:

- [x] Admin can manage bookstore catalog data without Postman for books, variations, media, stock, and categories.
- [ ] Manual browser smoke remains to verify a newly created active book appears in public catalog with a live backend.

## Phase FE-10: Admin Order Lifecycle And Manual Refund

Goal: replace pending-only orders page with full admin order operations.

Pages:

- [x] `AdminOrdersPage`.
- [x] Order detail drawer/modal.
- [x] `/admin/orders` route.
- [x] `/admin/orders-pending` redirects to `/admin/orders`.
- [x] Admin sidebar points to `/admin/orders`.

List filters:

- [x] status.
- [x] keyword.
- [x] fromDate.
- [x] toDate.
- [x] page/size.
- [x] Frontend blocks invalid date ranges.

Actions:

- [x] Confirm.
- [x] Mark packing.
- [x] Mark shipping.
- [x] Mark completed.
- [x] Cancel.
- [x] Mark refunded.

UX rules:

- [x] Only show valid next actions based on current status.
- [x] Paid cancellation guides admin to manual refund by showing refund instead of cancel.
- [x] Manual refund form requires amount, reason, note.
- [x] Show stock restore/manual refund behavior notes where useful.
- [x] Show payment group/payment status.
- [x] Backend `error.message` is displayed for invalid transitions/refund/cancel failures.

Acceptance:

- [ ] Manual smoke: admin can run COD order lifecycle end to end.
- [x] Invalid transitions are not offered in UI and backend errors are still handled.
- [x] Paid pre-shipping order can be marked refunded from the UI.
- [ ] Manual smoke remains to verify refund/cancel behavior against live backend data.

## Phase FE-11: Admin Users, Roles, And Permissions

Goal: expose practical user/RBAC management.

Users:

- [x] `/admin/users` route and admin sidebar item.
- [x] List/filter by keyword, role, active, locked, emailVerified.
- [x] Pagination with backend `PageResponse`.
- [x] Detail drawer with roles and account flags.
- [x] Lock/unlock user with confirmation.
- [x] Replace roles with USER/ADMIN.
- [x] Prevent self-lock/self-role-change in UI when current user id matches.
- [x] Disable lock/unlock/role edit for deleted users.

Permissions:

- [x] Existing role permissions page remains wired and is polished.
- [x] Role permission list is searchable and limited to USER/ADMIN roles returned by backend.
- [x] Direct user permission workspace accepts manual user id and `/admin/permissions?userId=<id>`.
- [x] Direct user permissions:
  - [x] list effective/direct/role permissions.
  - [x] grant permission with optional reason/expiresAt.
  - [x] revoke permission with confirmation.

Acceptance:

- [x] Admin can inspect and change access state safely in UI.
- [x] No seller/shop roles are shown.
- [ ] Manual browser smoke remains to verify user lock/unlock, role update, and direct permission grant/revoke against live backend.

## Phase FE-12: Admin Coupons And Promotions

Goal: admin can manage discounts without backend/Postman.

Coupons:

- [x] `/admin/discounts` route with Coupons tab.
- [x] `/admin/coupons` redirects to `/admin/discounts`.
- [x] List with backend `page`/`size`.
- [x] Create/update/deactivate.
- [x] Copy coupon code and link to checkout for testing.
- [x] Fields:
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

- [x] Promotions tab in `/admin/discounts`.
- [x] `/admin/promotions` redirects to `/admin/discounts`.
- [x] List with backend `page`/`size`.
- [x] Create/update/deactivate.
- [x] Product target picker using admin products.
- [x] Category target picker using categories.
- [x] Fields:
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

- [x] Date range.
- [x] Percent/fixed positive values.
- [x] Percent value max 100.
- [x] Non-negative amount/limit fields.
- [x] Required target.
- [x] Backend duplicate name/code errors displayed through `error.message`.

Acceptance:

- [x] Admin can manage coupon/promotion without Postman.
- [ ] Manual smoke remains to create coupon/promotion and test discount in checkout preview against live backend/cart.

## Phase FE-13: Admin Review Moderation

Goal: build a practical admin review moderation workspace for bookstore reviews. Admin can list/filter reviews, inspect review detail, approve/unapprove, hide/show, reply, and clear reply. Public product pages continue to show only approved, visible, non-deleted reviews. No seller/shop/merchant reply or moderation scope is introduced.

Routes and navigation:

- [x] Add `AdminReviewsPage`.
- [x] Add route `/admin/reviews`.
- [x] Add `Reviews` sidebar item in `AdminLayout`, placed after `Discounts` or before `Users`.
- [x] Keep `/admin/reviews` protected by the existing admin route guard.
- [x] Do not add broken placeholder routes or separate review settings pages.

API usage:

- [x] Use only `adminReviewsApi.js`.
- [x] List reviews with:
  - `getAdminReviews({ approved, visible, rating, keyword, productId, userId, page, size })`.
- [x] Moderate with:
  - `moderateReview(reviewId, { approved, visible })`.
- [x] Reply with:
  - `replyReview(reviewId, { adminReply })`.
- [x] Do not call `fetch` directly from components.
- [x] Do not add new backend endpoints.

List filters:

- [x] `approved`: all, approved, pending/unapproved.
- [x] `visible`: all, visible, hidden.
- [x] `rating`: all, 5, 4, 3, 2, 1.
- [x] `keyword`: free text search.
- [x] `productId`: numeric book id filter.
- [x] `userId`: customer id filter.
- [x] `page` and `size`, default `page=1`, `size=20`.
- [x] Filters are stored in URL query params so refresh/share keeps the same view.
- [x] Apply resets `page=1`.
- [x] Clear removes filters and resets page/size defaults.
- [x] Frontend validates numeric `productId`, `rating`, `page`, and `size` before API call.

Review list/table:

- [x] Render dense admin table/card rows with:
  - rating stars.
  - comment preview.
  - image count or first thumbnail.
  - book name.
  - SKU.
  - customer username and user id.
  - order id and order item id.
  - approved status.
  - visible status.
  - deleted state if `deletedAt` exists.
  - admin reply presence.
  - createdAt and updatedAt.
- [x] Use backend `PageResponse` for pagination.
- [x] Support loading, error, and empty states while keeping filters visible.
- [x] Display backend `error.message` for failed list/moderation/reply calls.

Review detail drawer:

- [x] Open detail drawer/modal from each row.
- [x] Show full review information:
  - review id.
  - rating.
  - full comment.
  - images as thumbnail grid.
  - productId, productVariationId, productName, sku.
  - userId and username.
  - orderId and orderItemId.
  - approved, visible, deletedAt.
  - moderatedBy, moderatedAt.
  - adminReply, repliedBy, repliedAt.
  - createdAt, updatedAt.
- [x] If review image URLs exist, clicking thumbnail opens a larger preview panel/modal.
- [x] If deleted, show disabled action state and clear explanation that deleted reviews should not be moderated back into public visibility.

Moderation actions:

- [x] Quick action `Approve & show` sends `{ approved: true, visible: true }`.
- [x] Quick action `Unapprove` sends `{ approved: false, visible: false }`.
- [x] Quick action `Hide` sends `{ approved: currentApproved, visible: false }`.
- [x] Quick action `Show` sends `{ approved: currentApproved, visible: true }`.
- [x] Detail drawer also has explicit approved/visible toggles with a save button.
- [x] Disable actions while request is in flight.
- [x] After success, update selected review detail from API response.
- [x] After success, refresh current list page so filters/counts stay accurate.
- [x] If backend rejects moderation, show `error.message` and keep local state unchanged.

Admin reply:

- [x] Reply form uses textarea max `2000` characters to match `ReviewReplyRequest`.
- [x] Trim reply before sending.
- [x] Submit non-blank reply with `{ adminReply }`.
- [x] Clear reply sends `{ adminReply: "" }`.
- [x] Show existing reply, repliedBy, and repliedAt in detail.
- [x] Make copy explicit: this is an admin reply, not shop/seller reply.
- [x] After reply/clear success, update selected detail and current row.

Public visibility verification:

- [x] Product detail public reviews remain driven by `GET /products/{slug}/reviews`.
- [ ] Admin-approved and visible review appears in public list after refresh.
- [ ] Hidden or unapproved review does not appear publicly.
- [ ] Admin reply appears on public product detail only when review is public.

Component structure:

- [x] Keep page code maintainable by extracting components if needed:
  - `AdminReviewFilters`.
  - `AdminReviewTable`.
  - `AdminReviewDetailDrawer`.
  - `AdminReviewModerationActions`.
  - `AdminReviewReplyForm`.
  - `ReviewImagePreview`.
- [x] Reuse existing `RatingStars` from customer review components if suitable.
- [x] Reuse `formatDateTime` and existing pagination helpers/patterns from admin orders/users.
- [x] Add small review mapper helpers only if backend response variants need normalization.

i18n:

- [x] Add EN/VI keys for:
  - reviews sidebar label.
  - review filters.
  - approved/unapproved/pending labels.
  - visible/hidden labels.
  - deleted review label.
  - rating labels.
  - moderation actions.
  - reply form.
  - clear reply confirmation.
  - review detail sections.
  - image preview alt text.
  - loading/error/empty states.
  - success messages.
- [x] Remove hardcoded user-facing strings from the new page.

Plan tracking:

- [x] Convert FE-13 to checklist during implementation.
- [x] Tick implemented FE-13 items after build passes.
- [x] Keep FE-14+ unchecked.
- [ ] Leave live backend smoke items unchecked until manually verified.

Test plan:

- [x] Run build:

```powershell
cd frontend
npm run build
```

- [ ] Manual smoke with backend:
  - Login admin and open `/admin/reviews`.
  - List loads with pagination.
  - Filter by approved, visible, rating, keyword, productId, userId.
  - Open review detail and verify product/customer/order metadata.
  - Approve and show a pending review.
  - Hide an approved review.
  - Unapprove a review.
  - Add admin reply.
  - Clear admin reply.
  - Backend errors display without crashing page.
  - Approved visible review appears on public product detail.
  - Hidden/unapproved review disappears from public product detail.
  - Admin reply appears publicly only for approved visible review.
  - Mobile/tablet review table and drawer remain usable.

Acceptance:

- [x] `/admin/reviews` exists and is reachable from admin sidebar.
- [x] Admin can list and filter reviews using backend-supported filters.
- [x] Admin can inspect review detail including book, customer, order item, images, and moderation metadata.
- [x] Admin can approve/unapprove and hide/show reviews.
- [x] Admin can add and clear `adminReply`.
- [ ] Public product detail only shows approved, visible, non-deleted reviews.
- [ ] Admin reply appears on public product detail after approval.
- [x] All API calls go through `adminReviewsApi.js`.
- [x] No seller/shop/merchant wording, role, route, or behavior is introduced.
- [x] `npm run build` passes.

Assumptions:

- Backend Phase 6 review APIs are already implemented.
- Backend admin review list supports only `approved`, `visible`, `rating`, `keyword`, `productId`, `userId`, `page`, and `size`.
- Review image upload is not part of FE-13; admin only views existing image metadata.
- Review deletion remains customer soft-delete flow; admin moderation uses visibility and approval only.
- Blank `adminReply` clears the reply.
- `ReviewResponse` has enough fields for admin list/detail without another detail endpoint.

## Phase FE-14: Admin Payments And Reconciliation

Goal: admin payment screen is operationally useful.

Backend prerequisite:

- [x] Add `GET /admin/payments/groups/{paymentGroupCode}`.
- [x] Protect admin payment group read with `PAYMENT_MANAGE_ALL` or `PAYMENT_READ_ALL`.
- [x] Reuse `PaymentGroupResponse`; no new DTO.
- [x] Keep existing reconcile endpoint unchanged.
- [x] No schema migration.
- [x] No checkout/retry/callback/IPN behavior change.

Frontend API:

- [x] Add `getAdminPaymentGroup(code)` in `adminPaymentsApi.js`.
- [x] Keep `reconcilePaymentGroup(code)`.
- [x] Stop using customer `getPaymentGroup()` in admin payments page.

Admin payments workspace:

- [x] Audit and replace existing simple `AdminPaymentsPage`.
- [x] Lookup payment group by code.
- [x] Support `/admin/payments?code=<paymentGroupCode>` initial lookup.
- [x] Show payment group detail:
  - payment code.
  - method.
  - status.
  - amount.
  - provider txn ref.
  - provider transaction id.
  - paidAt.
  - expiresAt.
  - payment URL/deeplink/QR availability.
- [x] Show payment rows:
  - payment id.
  - order code/id.
  - method.
  - status.
  - amount.
  - transaction id.
  - paidAt.
- [x] Show related orders from payment group response.
- [x] Link related orders to `/admin/orders?keyword=<orderCode>`.
- [x] Update `AdminOrdersPage` to read initial query params.
- [x] Reconcile payment group from admin page.
- [x] Show reconcile result:
  - before/after local status.
  - provider status.
  - changed flag.
  - provider txn ref.
  - checkedAt.
  - provider message.
- [x] Refresh payment group after reconcile.
- [x] Confirm before reconciling terminal statuses.
- [x] Display backend `error.message`.
- [x] Add EN/VI i18n keys.

Provider/callback visibility:

- [x] Do not add callback/IPN routes to customer or admin navigation.
- [x] Keep customer-facing payment result page unchanged.

Acceptance:

- [x] Admin can investigate payment groups by code.
- [x] Admin can see payment rows and related orders.
- [x] Admin can run reconcile.
- [x] Admin order link uses keyword query.
- [x] Admin payments page does not use customer self payment endpoint.
- [ ] Live provider smoke remains to run reconcile against sandbox data.

## Phase FE-15: UI System, Accessibility, And Responsiveness

Status: completed. commit `fd77970` on `feature/frontend`.

Goal: make the app feel finished, not stitched together.

Shared UI components:

- [x] Create `frontend/src/components/ui/`.
- [x] Add `Button`.
- [x] Add `IconButton` with required `aria-label`.
- [x] Add `Input`.
- [x] Add `Select`.
- [x] Add `Textarea`.
- [x] Add `Checkbox`.
- [x] Add `Toggle`.
- [x] Add `DateTimeInput`.
- [x] Add `Modal`.
- [x] Add `Drawer`.
- [x] Add `Table`.
- [x] Add `Pagination`.
- [x] Add `Tabs`.
- [x] Add toast provider and `useToast`.
- [x] Add `EmptyState`.
- [x] Add `ErrorState`.
- [x] Add `Skeleton`.
- [x] Add `Badge`.
- [x] Add `StatusPill`.
- [x] Add confirm dialog provider and `useConfirm`.

Adoption:

- [x] Wire `ToastProvider` and `ConfirmDialogProvider` in `App`.
- [x] Migrate admin payments page to shared UI primitives.
- [x] Replace all `window.confirm` usage in frontend pages/components with shared `ConfirmDialog`.
- [x] Use shared toast for admin payment reconcile and admin review moderation/reply.
- [x] Migrate all admin pages to shared UI primitives:
  - [x] `AdminProductsPage.jsx`
  - [x] `AdminCategoriesPage.jsx`
  - [x] `AdminOrdersPage.jsx`
  - [x] `AdminDiscountsPage.jsx`
  - [x] `AdminUsersPage.jsx`
  - [x] `AdminReviewsPage.jsx`
  - [x] `AdminPermissionsPage.jsx`
  - [x] `AdminPaymentsPage.jsx`
  - [x] `AdminDashboardPage.jsx`
  - [x] `AdminOrdersPendingPage.jsx`
- [x] Migrate customer pages to shared primitives:
  - [x] `OrdersPage.jsx` — 12 local helpers removed, OrderDetailModal→Drawer, modals→Modal
  - [x] `AccountPage.jsx` — 8 local helpers removed, gender→Select, checkbox→Checkbox
  - [x] `CheckoutPage.jsx` — 5 local helpers removed, select→Select, textarea→Textarea
  - [x] `CategoryPage.jsx` — 5 local helpers removed, mobile filter div→Drawer
  - [x] `CartPage.jsx` — 4 local helpers removed
  - [x] `PaymentResultPage.jsx` — 2 local helpers removed

Accessibility:

- [x] Modal/dialog has `role="dialog"` and `aria-modal`.
- [x] Modal/drawer close on `Escape`.
- [x] Modal/drawer include basic focus trap and return focus.
- [x] Form controls support label, hint, error, required, and `aria-describedby`.
- [x] Icon-only button requires `aria-label`.
- [x] Confirm flow no longer uses browser confirm dialogs.

Responsive:

- [x] Shared `Table` uses horizontal scroll with stable min width.
- [x] Shared buttons/loading states keep layout stable.
- [x] Mobile catalog filter drawer migration (`CategoryPage` mobile filter → shared `Drawer`).

Acceptance:

- [x] Common UI primitives exist for customer/admin adoption.
- [x] Main confirm flows use accessible confirm dialog.
- [x] Admin payments uses shared UI system.
- [x] All admin pages migrated to shared UI system.
- [x] All customer pages migrated to shared UI system.
- [x] No duplicate local `Button/Input/Panel/Pagination/Notice/EmptyState` helpers remain.
- [x] `npm run build` passes (2255 modules, 0 errors, 785kB bundle).

## Phase FE-16: Internationalization And Copy

Status: completed. commit `2dc488f` on `feature/frontend`.

Goal: keep Vietnamese/English support maintainable.

Tasks:

- [x] Audit `src/i18n.js`; split into locale files.
  - [x] Extracted 2004-line monolith into `locales/vi/translation.js` and `locales/en/translation.js`.
  - [x] `i18n.js` replaced with lean 43-line entry point; public API unchanged.
- [x] Add keys for all new pages:
  - [x] admin dashboard (`admin.dashboardTitle`, `admin.metricRevenue`, `admin.salesTrend`, etc.)
  - [x] orders/refunds (`admin.ordersTitle`, `admin.refundReason`, `admin.refundNote`, etc.)
  - [x] users (`admin.usersTitle`, `admin.lockUser`, `admin.roleEditor`, etc.)
  - [x] coupons/promotions (`admin.discountsTitle`, `admin.coupons`, `admin.promotions`, etc.)
  - [x] reviews (`admin.reviewsTitle`, `admin.approveShow`, `admin.saveModeration`, etc.)
  - [x] checkout preview (`checkout.cod`, `checkout.vnpay`, `checkout.momo`, `checkout.methodLabel.*`)
- [x] Remove hardcoded user-facing strings from components:
  - [x] `CheckoutPage.jsx` — payment method labels `"VNPay"/"MoMo"/"COD"` → `t("checkout.methodLabel.METHOD")`
  - [x] `AdminDiscountsPage.jsx` — enum options `PERCENT/FIXED/PRODUCT/CATEGORY` → `t("admin.discountTypeLabel.*")` / `t("admin.promotionScopeLabel.*")`
  - [x] `AdminProductsPage.jsx` — `"Featured"` badge → `t("admin.featured")`
- [x] Add missing keys: `checkout.methodLabel`, `admin.discountTypeLabel`, `admin.promotionScopeLabel`, `admin.bookFormat`, `admin.shippingInfo`, `admin.orderItems`, `admin.paidAt`.
- [x] Keep technical admin labels precise.

Acceptance:

- [x] Language switch works across all new screens.
- [x] No visible raw translation keys.
- [x] `npm run build` passes (2255 modules, 0 errors).

## Phase FE-17: Frontend Testing

Status: completed in current workspace. `npm run build`, `npm run test`, and `npm run test:e2e` pass.

Goal: add safety net for critical flows.

Add dependencies:

- [x] Vitest.
- [x] React Testing Library.
- [x] MSW for API mocking.
- [x] Playwright for E2E smoke.

Tooling:

- [x] Add `npm run test`.
- [x] Add `npm run test:watch`.
- [x] Add `npm run test:coverage`.
- [x] Add `npm run test:e2e`.
- [x] Add `npm run quality`.
- [x] Configure Vitest with `jsdom` and setup file.
- [x] Configure MSW server and shared mock bookstore data.
- [x] Configure Playwright with Vite dev server and mocked `/api/v1/**`.
- [x] Add shared `renderWithProviders()` test helper.
- [x] Add auth seeding helper for customer/admin tests.

Unit/component tests:

- [x] API client query builder, success envelope, normalized errors, refresh-on-401, and refresh failure auth expiry.
- [x] Auth storage supports backend token shapes.
- [x] Customer and admin route guards.
- [x] Checkout cart item selection helpers.
- [x] Catalog URL filters map to backend product query params.
- [x] Product detail renders book metadata and public reviews.
- [x] Checkout preview renders backend totals and coupon backend errors.
- [x] Admin dashboard renders live summary/book sections from mocked endpoints.
- [x] Admin order lifecycle action visibility for pending confirmation orders.
- [x] Coupon/promotion admin tabs render backend data.
- [x] Admin review moderation sends `{ approved, visible }`.
- [x] Admin payment lookup and reconcile result render.
- [ ] Auth modal login success/error component test can be expanded later.
- [ ] Review form validation component test can be expanded later.

E2E smoke tests:

- [x] Public browse homepage, catalog, and product detail.
- [x] Customer auth seed, cart, checkout preview, and orders.
- [x] Admin auth seed and dashboard.
- [x] Admin products list smoke.
- [x] Admin orders lifecycle action smoke.
- [x] Admin discounts coupon/promotion smoke.
- [x] Admin reviews smoke.
- [x] Admin payments lookup/reconcile smoke.
- [ ] Live backend manual smoke remains to be run separately.

Acceptance:

- [x] `npm run test` available.
- [x] `npm run test:e2e` available.
- [x] Tests use mocked backend responses and do not require backend/Testcontainers.
- [x] `npm run build` passes.
- [x] `npm run test` passes.
- [x] `npm run test:e2e` passes.
- [x] Core user/admin flows have smoke coverage.
- [x] No seller/shop/merchant wording or workflow added.

## Phase FE-18: Frontend Config, Build, And Deployment

Status: completed in current workspace. `npm run ci` passes.

Goal: production-practical frontend delivery.

Config:

- [x] `.env.example` documents:
  - [x] `VITE_API_BASE_URL`.
  - [x] `VITE_DEV_PROXY_TARGET`.
  - [x] Same-origin reverse proxy mode.
  - [x] Split frontend/backend domain mode.
- [x] Local dev uses Vite proxy to `/api/v1`.
- [x] `vite.config.js` reads `VITE_DEV_PROXY_TARGET`.
- [x] Production can point to deployed backend API through `VITE_API_BASE_URL`.
- [x] No frontend secrets are introduced.

Build:

- [x] `npm run build`.
- [x] `npm run preview`.
- [x] `npm run test`.
- [x] `npm run test:e2e`.
- [x] `npm run lint`.
- [x] `npm run lint:fix`.
- [x] `npm run format`.
- [x] `npm run format:check`.
- [x] `npm run ci`.
- [x] ESLint config added.
- [x] Prettier config added.
- [x] Prettier gate scoped to docs/config/test files to avoid unrelated full-source formatting churn.

Deployment docs:

- [x] `frontend/README.md` added.
- [x] Root `README.md` links to frontend docs.
- [x] Static hosting option documented.
- [x] Reverse proxy option documented.
- [x] Split-origin option documented.
- [x] CORS requirements documented.
- [x] Refresh cookie secure/sameSite/domain considerations documented.
- [x] Payment return URL configuration documented.
- [x] GitHub Actions frontend quality workflow added.
- [ ] Live production deployment verification remains pending.

Acceptance:

- [x] A developer can run and build FE using README instructions.
- [x] Production environment variables are clearly documented.
- [x] CI workflow runs frontend build, tests, E2E smoke, lint, and format check.
- [x] No runtime business behavior changed.
- [x] No seller/shop/merchant UI introduced.

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
