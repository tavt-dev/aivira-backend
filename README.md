# Aivira Backend

Backend API cho hệ thống e-commerce marketplace Aivira, xây dựng bằng Spring Boot. Project hiện là một monolith backend với phần API đã hoàn thiện ở xác thực người dùng, quản lý phiên đăng nhập, hồ sơ cá nhân, Hybrid RBAC và Seller Marketplace MVP. Các domain e-commerce như catalog, giỏ hàng, đơn hàng, thanh toán, khuyến mãi và review đã có entity nền để phát triển tiếp.

## Tổng Quan

`aivira-backend` tập trung vào:

- Đăng ký, xác minh email, đăng nhập, refresh token và đăng xuất.
- OTP qua email cho đăng ký tài khoản và quên mật khẩu.
- Quản lý access token/refresh token, session thiết bị, token rotation và phát hiện reuse refresh token.
- Bảo vệ API bằng Spring Security OAuth2 Resource Server với JWT.
- Quản lý hồ sơ người dùng hiện tại qua `/users/me`.
- Seller onboarding, shop profile, admin shop moderation và seller ownership guard nền.
- Nền tảng dữ liệu cho e-commerce: product, category, cart, order, payment, coupon, promotion, review.

## Công Nghệ

- Java `21`
- Spring Boot `4.0.1`
- Spring Web
- Spring Data JPA + Hibernate
- Spring Security + OAuth2 Resource Server
- MySQL
- Flyway
- Spring Mail
- Springdoc OpenAPI / Swagger UI
- Cloudinary SDK
- Lombok
- MapStruct
- Maven Wrapper
- Spotless
- JUnit 5, Mockito, AssertJ, Testcontainers

## Cấu Trúc Project

```text
src/main/java/com/tien/aivirabackend
├─ config/                 # Security, JWT decoder, OpenAPI, async, scheduling, cloudinary, seed data
├─ constant/               # Enum hệ thống: role, token, payment, order, coupon, media, ...
├─ controller/             # REST controllers: AuthenticationController, UserController
├─ domain/
│  ├─ dto/                 # ApiResponse, PageResponse, request/response DTO
│  ├─ entity/              # JPA entities theo domain auth/user/e-commerce
│  └─ mapper/              # MapStruct mapper
├─ exception/              # AppException, ErrorCode, GlobalExceptionHandler
├─ repository/             # Spring Data JPA repositories
└─ service/                # Business service interfaces và implementations
```

Các file cấu hình chính:

- `src/main/resources/application.yaml`: cấu hình chung, yêu cầu secret qua environment variable.
- `src/main/resources/application-local.yaml`: override local, dùng MySQL local mặc định và tắt `Secure` cho refresh-token cookie.
- `src/main/resources/application-dev.yaml`: cấu hình dev/staging, bật Flyway và Hibernate validate.
- `src/main/resources/application-prod.yaml`: cấu hình production, bắt buộc Flyway + Hibernate validate, không dùng `ddl-auto=update`.
- `src/main/resources/application-test.yaml`: cấu hình test, dùng Flyway/Hibernate validate và secret test.
- `src/main/resources/db/migration/V1__init_schema.sql`: migration khởi tạo schema và seed RBAC reference data.
- `src/main/resources/db/migration/V2__seller_marketplace.sql`: migration Seller Marketplace MVP, tạo bảng `shops` và bổ sung `SELLER_APPLY` cho role `USER`.
- `.env.example`: danh sách biến môi trường tham khảo.

## Trạng Thái Tính Năng

### Đã Có API

- Authentication:
  - Đăng ký tài khoản local.
  - Xác minh email bằng OTP.
  - Gửi lại OTP xác minh.
  - Đăng nhập.
  - Refresh token có rotation.
  - Logout một phiên.
  - Logout tất cả phiên.
  - Xem danh sách phiên đang hoạt động.
  - Thu hồi một phiên theo session ID.
  - Quên mật khẩu và đặt lại mật khẩu bằng OTP.
- User profile:
  - Lấy profile hiện tại.
  - Cập nhật `firstName`, `lastName`, `gender`.
  - Đổi mật khẩu và thu hồi session cũ.
  - Vô hiệu hóa tài khoản hiện tại.
  - Upload avatar lên Cloudinary, chuẩn hóa ảnh vuông và cập nhật `avatarUrl`.
- Admin permission:
  - Xem danh sách permission hệ thống.
  - Xem danh sách role kèm permission.
  - Xem/cập nhật permission của từng role.
  - Xem effective permissions của từng user.
  - Grant direct permission cho một user cụ thể.
  - Revoke direct permission đang active của user.
- Seller marketplace:
  - User đăng ký mở shop và nhận trạng thái `PENDING`.
  - Seller xem/cập nhật hồ sơ shop, resubmit shop bị từ chối và upload logo shop lên Cloudinary.
  - Admin xem danh sách shop, duyệt, từ chối, khóa và mở khóa shop.
  - Khi admin duyệt shop, owner được gán role `SELLER`.
  - Seller dashboard Phase 3 trả summary placeholder với số liệu `0` cho đến khi catalog/order hoàn thiện.
- Catalog:
  - Public xem category list/tree và search/detail product đang `ACTIVE`.
  - Admin tạo, cập nhật và soft delete category.
  - Seller tạo/sửa/soft delete product trong shop đã `APPROVED`, quản lý variation, stock và media.
  - Seller submit product để admin duyệt; admin approve/reject product trước khi public.
  - Product media upload lên Cloudinary, media primary cập nhật thumbnail product.
- Address, cart, checkout và payment v1:
  - Customer quản lý address book, địa chỉ mặc định và soft delete địa chỉ.
  - Customer quản lý cart active, add/update/remove/clear item và merge item trùng variation.
  - Checkout selected cart item, split order theo shop, snapshot sản phẩm/địa chỉ/pricing và trừ stock trong transaction.
  - Payment group gom nhiều order trong một lần checkout, hỗ trợ `COD`, `VNPAY`, `MOMO`.
  - VNPay/MoMo có create payment URL, return/IPN callback verify signature, idempotency và expire pending payment.

### Đã Có Entity Nền

- User/Auth: `User`, `Role`, `Permission`, `UserPermission`, `Address`, `UserOtp`, `RefreshToken`, `InvalidatedToken`
- Permission mappings: `user_roles`, `role_permissions`, `user_permissions`
- Marketplace: `Shop`
- Catalog: `Category`, `Product`, `ProductVariation`, `ProductMedia`
- Transaction: `Cart`, `CartItem`, `Order`, `OrderItem`, `Payment`, `PaymentGroup`, `PaymentCallback`
- Discount: `Coupon`, `CouponUsage`, `Promotion`
- Review: `Review`, `ReviewImage`

Các module discount/review hiện mới có entity nền, chưa có API nghiệp vụ đầy đủ. Order Phase 5 hiện tập trung vào tạo đơn từ checkout và payment status; order management cho customer/seller/admin sẽ làm tiếp ở Phase 7.

## API

Base URL mặc định:

```text
http://localhost:8080/api/v1
```

Swagger:

- Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v1/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/api/v1/v3/api-docs.yaml`

### Public Endpoints

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/auth/register` | Đăng ký tài khoản mới và gửi OTP qua email |
| `POST` | `/auth/verify-user` | Xác minh email bằng OTP đăng ký |
| `POST` | `/auth/resend-verification` | Gửi lại OTP xác minh |
| `POST` | `/auth/token` | Đăng nhập, trả access token và refresh token |
| `POST` | `/auth/refresh-token` | Refresh access token và rotate refresh token |
| `POST` | `/auth/logout` | Thu hồi refresh token hiện tại |
| `POST` | `/auth/forgot-password` | Gửi OTP reset mật khẩu |
| `POST` | `/auth/reset-password` | Đặt lại mật khẩu bằng OTP |

### Protected Endpoints

Cần header:

```http
Authorization: Bearer <access-token>
```

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/auth/logout-all` | Thu hồi toàn bộ refresh-token session của user hiện tại |
| `GET` | `/auth/sessions` | Lấy danh sách refresh-token session đang hoạt động |
| `DELETE` | `/auth/sessions/{sessionId}` | Thu hồi một session cụ thể |
| `GET` | `/users/me` | Lấy profile user hiện tại |
| `PUT` | `/users/me` | Cập nhật profile cơ bản |
| `PUT` | `/users/me/avatar` | Upload avatar lên Cloudinary, nhận multipart field `avatar` |
| `PUT` | `/users/me/password` | Đổi mật khẩu |
| `POST` | `/users/me/deactivate` | Vô hiệu hóa và đánh dấu xóa tài khoản hiện tại |

### Seller Shop Endpoints

Cần bearer token và permission phù hợp như `SELLER_APPLY`, `SHOP_READ_SELF`, `SHOP_UPDATE_SELF` hoặc `DASHBOARD_READ_SELLER`.

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/seller/shop/apply` | Tạo shop application trạng thái `PENDING` cho user hiện tại |
| `GET` | `/seller/shop` | Lấy shop của user hiện tại |
| `PUT` | `/seller/shop` | Cập nhật hồ sơ shop khi shop chưa bị khóa/inactive |
| `POST` | `/seller/shop/resubmit` | Gửi lại shop bị `REJECTED` để admin duyệt lại |
| `PUT` | `/seller/shop/logo` | Upload logo shop qua multipart field `logo` |
| `GET` | `/seller/dashboard` | Lấy dashboard placeholder Phase 3 của seller |

### Admin Permission Endpoints

Cần bearer token và permission phù hợp như `PERMISSION_MANAGE`, `ROLE_MANAGE` hoặc nhóm `USER_PERMISSION_*`.

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/admin/permissions` | Lấy toàn bộ permission hệ thống |
| `GET` | `/admin/roles` | Lấy danh sách role kèm permission |
| `GET` | `/admin/roles/{roleCode}/permissions` | Lấy permission của một role |
| `PUT` | `/admin/roles/{roleCode}/permissions` | Thay thế permission của một role |
| `GET` | `/admin/users/{userId}/permissions` | Lấy role permissions, direct permissions và effective permissions của user |
| `POST` | `/admin/users/{userId}/permissions` | Grant direct permission cho user |
| `DELETE` | `/admin/users/{userId}/permissions/{permissionCode}` | Revoke direct permission đang active của user |

### Admin Shop Endpoints

Cần bearer token và permission phù hợp như `SHOP_READ_ALL`, `SHOP_APPROVE`, `SHOP_REJECT`, `SHOP_LOCK`, `SHOP_UNLOCK` hoặc `SHOP_MANAGE_ALL`.

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/admin/shops` | Lấy danh sách shop có pagination, filter `status`, `keyword` |
| `GET` | `/admin/shops/{shopId}` | Lấy chi tiết shop |
| `PUT` | `/admin/shops/{shopId}/approve` | Duyệt shop `PENDING` và gán role `SELLER` cho owner |
| `PUT` | `/admin/shops/{shopId}/reject` | Từ chối shop `PENDING`, body có `reason` |
| `PUT` | `/admin/shops/{shopId}/lock` | Khóa shop `APPROVED`, body có `reason` |
| `PUT` | `/admin/shops/{shopId}/unlock` | Mở khóa shop `LOCKED` về `APPROVED` |

### Catalog Endpoints

Public:

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/categories` | Lấy danh sách category active/visible |
| `GET` | `/categories/tree` | Lấy cây category active/visible |
| `GET` | `/products` | Search product public theo `keyword`, `categorySlug`, `shopSlug`, `brand`, `minPrice`, `maxPrice`, `available`, `sort`, `page`, `size` |
| `GET` | `/products/{slug}` | Lấy chi tiết product public |

Seller product, cần shop đã `APPROVED` và permission phù hợp:

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/seller/products` | Lấy product của shop hiện tại, filter `status`, `keyword` |
| `GET` | `/seller/products/{productId}` | Lấy chi tiết product của shop hiện tại |
| `POST` | `/seller/products` | Tạo product `DRAFT` kèm ít nhất một variation |
| `PUT` | `/seller/products/{productId}` | Cập nhật product và đưa về `DRAFT` nếu đang chờ duyệt/đã public |
| `DELETE` | `/seller/products/{productId}` | Soft delete product về `INACTIVE` |
| `POST` | `/seller/products/{productId}/submit-review` | Gửi product `DRAFT`/`REJECTED` sang `PENDING_REVIEW` |
| `POST` | `/seller/products/{productId}/media` | Upload ảnh product multipart field `media` |
| `PUT` | `/seller/products/{productId}/media/{mediaId}` | Cập nhật alt text, sort order, primary hoặc active của media |
| `DELETE` | `/seller/products/{productId}/media/{mediaId}` | Soft delete media |
| `POST` | `/seller/products/{productId}/variations` | Thêm variation |
| `PUT` | `/seller/products/{productId}/variations/{variationId}` | Cập nhật variation |
| `DELETE` | `/seller/products/{productId}/variations/{variationId}` | Soft delete variation |
| `PUT` | `/seller/products/{productId}/variations/{variationId}/stock` | Cập nhật tồn kho variation |

Admin catalog/product:

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/admin/categories` | Tạo category |
| `PUT` | `/admin/categories/{categoryId}` | Cập nhật category |
| `DELETE` | `/admin/categories/{categoryId}` | Soft delete category |
| `GET` | `/admin/products` | Lấy danh sách product, filter `status`, `shopId`, `categoryId`, `keyword` |
| `GET` | `/admin/products/{productId}` | Lấy chi tiết product |
| `PUT` | `/admin/products/{productId}/approve` | Duyệt product `PENDING_REVIEW` thành `ACTIVE` |
| `PUT` | `/admin/products/{productId}/reject` | Từ chối product `PENDING_REVIEW`, body có `reason` |

### Address, Cart, Checkout Và Payment Endpoints

Address, cần bearer token:

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/users/me/addresses` | Lấy danh sách địa chỉ active của user hiện tại |
| `POST` | `/users/me/addresses` | Tạo địa chỉ mới |
| `PUT` | `/users/me/addresses/{addressId}` | Cập nhật địa chỉ thuộc user hiện tại |
| `DELETE` | `/users/me/addresses/{addressId}` | Soft delete địa chỉ |
| `PUT` | `/users/me/addresses/{addressId}/default` | Đặt địa chỉ mặc định |

Cart, cần bearer token:

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/cart` | Lấy giỏ hàng active của user hiện tại |
| `POST` | `/cart/items` | Thêm variation vào cart; trùng variation thì cộng quantity |
| `PUT` | `/cart/items/{cartItemId}` | Cập nhật quantity |
| `DELETE` | `/cart/items/{cartItemId}` | Xóa một cart item |
| `DELETE` | `/cart/items` | Xóa toàn bộ cart item |

Checkout và payment:

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/checkout` | Checkout các `cartItemIds` được chọn, split order theo shop và tạo payment group |
| `GET` | `/payments/groups/{paymentGroupCode}` | Lấy trạng thái payment group của user hiện tại |
| `GET` | `/payments/{paymentId}` | Lấy payment allocation của user hiện tại |
| `POST` | `/payments/groups/{paymentGroupCode}/retry` | Retry payment online bị fail/cancel/expired |
| `GET` | `/payments/vnpay/return` | VNPay Return URL public, verify signature |
| `GET` | `/payments/vnpay/ipn` | VNPay IPN public, verify signature và xử lý idempotent |
| `POST` | `/payments/momo/ipn` | MoMo IPN public, verify signature và xử lý idempotent |

## Payload Mẫu

### Đăng Ký

```json
{
  "username": "postman_user",
  "password": "Password123!",
  "email": "postman_user@example.com",
  "firstName": "Test",
  "lastName": "User"
}
```

### Xác Minh User

```json
{
  "email": "postman_user@example.com",
  "otpCode": "123456"
}
```

### Đăng Nhập

```json
{
  "username": "postman_user",
  "password": "Password123!"
}
```

### Refresh Token

Refresh token được ưu tiên đọc từ cookie `refreshToken`. Nếu `AUTH_REFRESH_TOKEN_BODY_ENABLED=true`, có thể gửi qua body:

```json
{
  "refreshToken": "<refresh-token>"
}
```

### Đổi Mật Khẩu

```json
{
  "currentPassword": "Password123!",
  "newPassword": "NewPassword123!",
  "confirmPassword": "NewPassword123!"
}
```

### Cập Nhật Profile

```json
{
  "firstName": "Updated",
  "lastName": "User",
  "gender": "MALE"
}
```

### Đăng Ký Mở Shop

```json
{
  "shopName": "Aivira Fashion",
  "description": "Thời trang nữ tuyển chọn",
  "businessEmail": "shop@example.com",
  "phoneNumber": "0900000000",
  "legalName": "Aivira Fashion LLC",
  "taxCode": "0312345678",
  "pickupAddressLine": "123 Nguyễn Trãi",
  "pickupWard": "Phường 7",
  "pickupDistrict": "Quận 5",
  "pickupCity": "TP. Hồ Chí Minh"
}
```

### Từ Chối Hoặc Khóa Shop

```json
{
  "reason": "Thông tin xác minh chưa đầy đủ."
}
```

### Tạo Category

```json
{
  "categoryName": "Thời trang nữ",
  "description": "Danh mục thời trang nữ",
  "displayOrder": 1,
  "parentId": null,
  "active": true,
  "visible": true
}
```

### Tạo Product Seller

```json
{
  "sku": "DRESS-001",
  "productName": "Đầm midi Aivira",
  "description": "Đầm midi chất liệu cotton cao cấp",
  "brand": "Aivira",
  "material": "Cotton",
  "categoryId": 1,
  "price": 399000,
  "originalPrice": 499000,
  "weight": 0.35,
  "variations": [
    {
      "sku": "DRESS-001-BLACK-M",
      "color": "Black",
      "size": "M",
      "additionalPrice": 0,
      "stockQuantity": 20
    }
  ]
}
```

### Tạo Địa Chỉ

```json
{
  "recipientName": "Nguyễn Văn A",
  "phoneNumber": "0900000000",
  "addressLine": "123 Nguyễn Trãi",
  "ward": "Phường 7",
  "district": "Quận 5",
  "city": "TP. Hồ Chí Minh",
  "defaultAddress": true
}
```

### Thêm Cart Item

```json
{
  "productVariationId": 1,
  "quantity": 2
}
```

### Checkout

```json
{
  "addressId": 1,
  "cartItemIds": [1, 2],
  "paymentMethod": "VNPAY",
  "notes": "Giao giờ hành chính"
}
```

`paymentMethod` hỗ trợ `COD`, `VNPAY`, `MOMO`. Với multi-seller cart, checkout tạo nhiều order nhưng gom vào một `paymentGroupCode`.

## Response Chuẩn

Mọi API trả về theo `ApiResponse<T>`.

Thành công:

```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {},
  "timestamp": 1766558400000
}
```

Thất bại:

```json
{
  "success": false,
  "errorCode": "E2107",
  "message": "Token không hợp lệ.",
  "timestamp": 1766558400000
}
```

Lỗi validation có thể trả thêm chi tiết theo field trong `data`.

## Bảo Mật Và Token

- Access token và refresh token là JWT ký bằng HMAC `HS256`.
- JWT dùng `issuer`, `jti`, `token_type`, `user_id`, `token_version`, `scope`.
- `scope` chứa role theo dạng `ROLE_USER`, `ROLE_ADMIN`, `ROLE_SELLER`.
- Access token có thời hạn theo `JWT_VALID_DURATION`, mặc định `3600` giây.
- Refresh token có thời hạn theo `JWT_REFRESHABLE_DURATION`, mặc định `36000` giây.
- Refresh token được lưu trong bảng `refresh_tokens` bằng hash SHA-256, kèm `jti`, `familyId`, thiết bị, IP, thời gian hết hạn và trạng thái revoke.
- Khi refresh thành công, refresh token cũ bị revoke và token mới giữ cùng `familyId`.
- Nếu refresh token đã revoke bị dùng lại, hệ thống coi là reuse/security breach và revoke toàn bộ token trong cùng `familyId`.
- Khi đổi mật khẩu, logout all hoặc deactivate account, hệ thống tăng `token_version` của user và revoke refresh token.
- `CustomJwtDecoder` từ chối access token hết hạn, sai issuer, sai chữ ký, bị invalidated hoặc có `token_version` cũ.
- Đăng nhập chỉ cho user đã xác minh email, active, không bị khóa và chưa bị đánh dấu xóa.
- Brute-force login lockout được cấu hình bằng nhóm biến `AUTH_BRUTE_FORCE_*`.

## Refresh Token Cookie

Sau login hoặc refresh token, backend ghi refresh token vào cookie.

Các thuộc tính mặc định:

- Tên cookie: `refreshToken`
- Path: `/api/v1/auth`
- `HttpOnly=true`
- `Secure=true`
- `SameSite=Lax`
- Max-Age bằng `JWT_REFRESHABLE_DURATION`

Với local HTTP, dùng profile `local` để override `auth.refresh-token.cookie-secure=false`.

## OTP Và Email

- OTP dùng cho hai loại: `REGISTER` và `RESET_PASSWORD`.
- OTP hiện tại là 6 chữ số.
- OTP mặc định hết hạn sau 10 phút trong flow đăng ký và reset mật khẩu.
- Gửi lại OTP bị giới hạn tần suất, hiện tối thiểu 1 phút giữa hai lần gửi.
- Email gửi bất đồng bộ qua `emailTaskExecutor`.
- Template email:
  - `src/main/resources/templates/email/registration-otp.html`
  - `src/main/resources/templates/email/forgot-password-otp.html`
- Job cleanup chạy theo cron `AUTH_CLEANUP_CRON`, mặc định mỗi giờ, để xóa refresh token hết hạn và OTP hết hạn/đã dùng cũ.

## Database

Tạo database MySQL:

```sql
CREATE DATABASE aivira CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Các bảng lõi theo entity hiện tại:

- Auth/User: `users`, `roles`, `user_roles`, `addresses`, `user_otp`, `refresh_tokens`, `invalidated_tokens`
- Marketplace: `shops`
- Catalog: `categories`, `products`, `product_variations`, `product_media`
- Transaction: `carts`, `cart_items`, `orders`, `order_items`, `payments`
- Discount: `coupons`, `coupon_usages`, `promotions`
- Review: `reviews`, `review_images`

Project hiện dùng Flyway để version schema. Migration khởi tạo nằm ở:

```text
src/main/resources/db/migration/V1__init_schema.sql
```

Migration Phase 3 Seller Marketplace nằm ở:

```text
src/main/resources/db/migration/V2__seller_marketplace.sql
```

`spring.jpa.hibernate.ddl-auto` mặc định là `validate`. Production không dùng `ddl-auto=update`; mọi thay đổi schema mới phải đi qua migration versioned. Flyway cũng seed dữ liệu RBAC nền gồm `roles`, `permissions` và `role_permissions`. Admin account vẫn được seed qua `ApplicationRunner` khi bật `SEED_ENABLED=true` vì cần password lấy từ environment variable.

## Biến Môi Trường

Spring Boot không tự nạp `.env` theo mặc định. Cần export biến môi trường trong shell/IDE hoặc cấu hình trực tiếp trong run configuration.

| Biến | Mặc định | Ghi chú |
|---|---|---|
| `SERVER_PORT` | `8080` | Port server |
| `DB_URL` | `jdbc:mysql://localhost:3306/aivira?...` | JDBC URL |
| `USERNAME_DB` | Không có | Bắt buộc |
| `PASSWORD_DB` | Không có | Bắt buộc |
| `FLYWAY_ENABLED` | `true` | Bật/tắt Flyway migration |
| `FLYWAY_LOCATIONS` | `classpath:db/migration` | Vị trí migration |
| `FLYWAY_BASELINE_ON_MIGRATE` | `false` | Không tự baseline production mặc định |
| `DDL_AUTO` | `validate` | Hibernate chỉ validate schema |
| `SHOW_SQL` | `false` | In SQL log |
| `JWT_SIGNER_KEY` | Không có | Bắt buộc, nên đủ dài cho HS256 |
| `JWT_ISSUER` | `aivira.com` | JWT issuer |
| `JWT_VALID_DURATION` | `3600` | Access token lifetime, đơn vị giây |
| `JWT_REFRESHABLE_DURATION` | `36000` | Refresh token lifetime, đơn vị giây |
| `PUBLIC_ENDPOINTS` | Xem `application.yaml` | Danh sách route public |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Origin frontend được phép |
| `CORS_ALLOWED_METHODS` | `GET,POST,PUT,PATCH,DELETE,OPTIONS` | HTTP methods |
| `CORS_ALLOWED_HEADERS` | `Authorization,Content-Type,Accept,Origin,X-Requested-With` | Headers |
| `AUTH_REFRESH_TOKEN_BODY_ENABLED` | `true` | Cho phép nhận refresh token từ body |
| `AUTH_REFRESH_TOKEN_COOKIE_NAME` | `refreshToken` | Tên cookie refresh token |
| `AUTH_REFRESH_TOKEN_COOKIE_PATH` | `/api/v1/auth` | Cookie path |
| `AUTH_REFRESH_TOKEN_COOKIE_SAME_SITE` | `Lax` | SameSite |
| `AUTH_REFRESH_TOKEN_COOKIE_SECURE` | `true` | Bật Secure cookie |
| `AUTH_REFRESH_TOKEN_COOKIE_HTTP_ONLY` | `true` | Bật HttpOnly |
| `AUTH_BRUTE_FORCE_MAX_ATTEMPTS` | `5` | Số lần sai mật khẩu trước khi lock |
| `AUTH_BRUTE_FORCE_WINDOW_MINUTES` | `15` | Cửa sổ tính failed login |
| `AUTH_BRUTE_FORCE_LOCK_MINUTES` | `15` | Thời gian khóa |
| `AUTH_CLEANUP_CRON` | `0 0 * * * *` | Cron cleanup auth data |
| `AUTH_CLEANUP_OTP_USED_RETENTION_HOURS` | `24` | Giữ OTP đã dùng trong bao lâu |
| `APP_UPLOAD_ENABLED` | `true` | Bật/tắt validate upload |
| `APP_UPLOAD_MAX_IMAGE_SIZE` | `5242880` | Size ảnh tối đa |
| `APP_UPLOAD_MAX_DOCUMENT_SIZE` | `10485760` | Size document tối đa |
| `APP_UPLOAD_ALLOWED_IMAGE_TYPES` | `image/jpeg,image/png,image/gif,image/webp` | MIME ảnh được phép |
| `APP_UPLOAD_ALLOWED_DOCUMENT_TYPES` | `application/pdf` | MIME document được phép |
| `MAIL_HOST` | `smtp-relay.brevo.com` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `BREVO_SMTP_LOGIN` | Không có | SMTP username |
| `BREVO_SMTP_KEY` | Không có | SMTP password/API key |
| `MAIL_FROM` | `***REMOVED***` | Sender email |
| `MAIL_FROM_NAME` | `Aivira Store` | Sender display name |
| `CLOUDINARY_CLOUD_NAME` | Không có | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Không có | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Không có | Cloudinary API secret |
| `CLOUDINARY_SECURE` | `true` | Bật secure URL cho Cloudinary |
| `CLOUDINARY_AVATAR_FOLDER` | `aivira/users` | Folder gốc lưu avatar |
| `CLOUDINARY_PRODUCT_MEDIA_FOLDER` | `aivira/products` | Folder gốc lưu product media |
| `PAYMENT_PENDING_TTL_MINUTES` | `15` | Thời gian giữ pending online payment trước khi expire |
| `PAYMENT_EXPIRY_SCAN_DELAY_MS` | `60000` | Chu kỳ scan payment pending quá hạn |
| `VNPAY_ENABLED` | `false` | Bật VNPay sandbox adapter |
| `VNPAY_PAYMENT_URL` | Sandbox URL | URL cổng thanh toán VNPay |
| `VNPAY_TMN_CODE` | Rỗng | VNPay merchant TMN code |
| `VNPAY_HASH_SECRET` | Rỗng | Secret ký/verify VNPay |
| `VNPAY_RETURN_URL` | Rỗng | Return URL frontend/backend gửi sang VNPay |
| `VNPAY_IPN_URL` | Rỗng | IPN URL cấu hình với VNPay |
| `MOMO_ENABLED` | `false` | Bật MoMo sandbox adapter |
| `MOMO_ENDPOINT` | Sandbox URL | Endpoint `/v2/gateway/api/create` |
| `MOMO_PARTNER_CODE` | Rỗng | MoMo partner code |
| `MOMO_ACCESS_KEY` | Rỗng | MoMo access key |
| `MOMO_SECRET_KEY` | Rỗng | Secret ký/verify MoMo |
| `MOMO_REDIRECT_URL` | Rỗng | Redirect URL sau thanh toán MoMo |
| `MOMO_IPN_URL` | Rỗng | IPN URL nhận kết quả MoMo |
| `MOMO_REQUEST_TYPE` | `payWithMethod` | Request type MoMo |
| `SEED_ENABLED` | `false` | Bật seed role/admin |
| `SEED_ADMIN_USERNAME` | Rỗng | Username admin seed |
| `SEED_ADMIN_PASSWORD` | Rỗng | Password admin seed |
| `SEED_ADMIN_EMAIL` | Rỗng | Email admin seed |

### Upload Config

Bind qua prefix `app.upload`.

| Property | Mặc định |
|---|---|
| `app.upload.enabled` | `true` |
| `app.upload.max-image-size` | `5242880` |
| `app.upload.max-document-size` | `10485760` |
| `app.upload.allowed-image-types` | `image/jpeg,image/png,image/gif,image/webp` |
| `app.upload.allowed-document-types` | `application/pdf` |

Avatar upload hiện validate file không rỗng, giới hạn size theo `app.upload.max-image-size`, kiểm tra MIME whitelist và magic bytes. Ảnh được upload vào Cloudinary theo folder `{CLOUDINARY_AVATAR_FOLDER}/{userId}/avatar`, public ID sinh ngẫu nhiên, transformation avatar mặc định là ảnh vuông `400x400`.

Product media upload dùng cùng validate ảnh, upload vào Cloudinary theo folder `{CLOUDINARY_PRODUCT_MEDIA_FOLDER}/{shopId}/{productId}` với transformation ảnh vuông `1200x1200`.

## Chạy Local

Yêu cầu:

- JDK `21`
- MySQL `8+`
- Maven hoặc Maven Wrapper
- Docker Desktop nếu muốn chạy integration test MySQL Testcontainers
- SMTP account nếu muốn test gửi email thật
- Cloudinary credentials nếu dùng upload media

Windows PowerShell:

```powershell
$env:JAVA_HOME="C:\Users\Admin\.jdks\ms-21.0.7"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:USERNAME_DB="root"
$env:PASSWORD_DB="your_password"
$env:JWT_SIGNER_KEY="your-very-long-secret-key-at-least-32-chars"
$env:BREVO_SMTP_LOGIN="your_smtp_login"
$env:BREVO_SMTP_KEY="your_smtp_key"
$env:CLOUDINARY_CLOUD_NAME="your_cloud"
$env:CLOUDINARY_API_KEY="your_key"
$env:CLOUDINARY_API_SECRET="your_secret"
$env:CLOUDINARY_AVATAR_FOLDER="aivira/users"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

macOS/Linux:

```bash
export USERNAME_DB=root
export PASSWORD_DB=your_password
export JWT_SIGNER_KEY=your-very-long-secret-key-at-least-32-chars
export BREVO_SMTP_LOGIN=your_smtp_login
export BREVO_SMTP_KEY=your_smtp_key
export CLOUDINARY_CLOUD_NAME=your_cloud
export CLOUDINARY_API_KEY=your_key
export CLOUDINARY_API_SECRET=your_secret
export CLOUDINARY_AVATAR_FOLDER=aivira/users
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Nếu không dùng profile `local`, cookie refresh token mặc định có `Secure=true`, trình duyệt sẽ không lưu cookie đó trên HTTP local.

Nếu database local đã từng được Hibernate tạo bằng `ddl-auto=update`, nên tạo database mới hoặc baseline thủ công trước khi dùng Flyway. Mặc định project không bật `FLYWAY_BASELINE_ON_MIGRATE` để tránh che lấp schema production chưa được kiểm soát.

## Seed Role Và Admin

Bật seed bằng biến:

```text
SEED_ENABLED=true
SEED_ADMIN_USERNAME=admin
SEED_ADMIN_PASSWORD=AdminPassword123!
SEED_ADMIN_EMAIL=admin@example.com
```

Khi startup, seeder sẽ:

- Đảm bảo permission/role mặc định idempotent nếu cần; RBAC reference data chính đã có trong Flyway migration.
- Tạo admin local account nếu username/email chưa tồn tại.
- Admin được tạo với `emailVerified=true` và `isActive=true`.

## Test Và Format

Chạy test:

```bash
./mvnw test
```

Windows:

```powershell
$env:JAVA_HOME="C:\Users\Admin\.jdks\ms-21.0.7"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

Test hiện có:

- Smoke test `contextLoads` với Flyway/Hibernate validate.
- Integration test nền MySQL Testcontainers cho migration, auth, profile, session, token rotation và refresh token reuse.
- Unit test `RefreshTokenCookieServiceTest`.
- Unit test `AuthenticationServiceImplTest` cho flow account chưa verify và lockout sau failed login.
- Unit test RBAC/user permission, authorization resolver, user service và upload validator.
- Unit test Seller Marketplace cho shop apply, chống tạo shop thứ hai, resubmit, lock/unlock, approve và gán role `SELLER`.
- Migration test kiểm tra role `USER` có quyền `SELLER_APPLY`.

Integration test sẽ chạy khi Docker daemon khả dụng. Nếu Docker chưa chạy, Testcontainers test được skip để unit test và build local vẫn chạy được.

Format code:

```bash
./mvnw spotless:apply
```

## Lưu Ý Hiện Tại

- Avatar cũ trên Cloudinary hiện được giữ lại khi user upload avatar mới; chưa có cleanup job cho media cũ.
- Shop logo cũ trên Cloudinary hiện được giữ lại khi seller upload logo mới; chưa có cleanup job cho media cũ.
- Product media cũ trên Cloudinary hiện được giữ lại khi seller update/delete media; chưa có cleanup job cho media cũ/orphan.
- Seller dashboard Phase 3 chỉ trả số liệu placeholder `0` vì order service chưa được triển khai.
- Discount/review mới có entity, chưa có API nghiệp vụ đầy đủ.
- Order management đầy đủ cho customer/seller/admin vẫn để Phase 7; Phase 5 hiện chỉ tạo order từ checkout và expose payment status.
- Đã có Flyway migration versioning từ `V1__init_schema.sql`; các thay đổi schema tiếp theo phải tạo migration mới.
- `.env` chỉ là file tham khảo, app không tự load file này nếu không thêm cơ chế load riêng.

## Roadmap Phát Triển Toàn Bộ Project

Roadmap này định hướng Aivira từ backend e-commerce nền tảng hiện tại thành một marketplace thực tế nhiều seller. Mục tiêu là có thể bóc task theo từng phase, triển khai dần nhưng vẫn giữ kiến trúc nhất quán.

### Tầm Nhìn Sản Phẩm

Aivira sẽ phát triển theo mô hình marketplace:

- `Guest`: xem danh mục, tìm kiếm sản phẩm, xem chi tiết sản phẩm, đăng ký và đăng nhập.
- `Customer`: quản lý hồ sơ, địa chỉ, giỏ hàng, checkout, thanh toán, theo dõi đơn hàng, review, wishlist.
- `Seller`: đăng ký shop, quản lý sản phẩm, biến thể, tồn kho, media, đơn hàng và khuyến mãi của shop mình.
- `Admin`: quản lý toàn hệ thống, user, seller, shop, sản phẩm, đơn hàng, thanh toán, refund, review, promotion, report và cấu hình hệ thống.

Flow mục tiêu của v1:

```text
catalog -> cart -> checkout -> payment -> order -> shipping -> completed -> review
```

Nguyên tắc thiết kế:

- Hoàn thiện end-to-end flow trước, tối ưu nâng cao sau.
- Mọi nghiệp vụ tiền, đơn hàng, thanh toán, tồn kho phải có trạng thái rõ ràng và test bao phủ.
- Order/payment/audit không hard delete.
- Seller chỉ được thao tác dữ liệu thuộc shop của mình.
- Admin có quyền điều phối và kiểm duyệt toàn hệ thống.

### Account, Role Và Permission

Project đã triển khai nền Hybrid RBAC trên role `USER`, `SELLER`, `ADMIN`. Role vẫn là nguồn quyền chính, đồng thời hệ thống hỗ trợ gán direct permission cho từng user để xử lý các quyền đặc biệt hoặc tạm thời mà không cần tạo role mới.

```text
Effective permissions = role permissions + active user direct permissions
```

Mô hình RBAC chuẩn:

- `users`: người dùng hệ thống.
- `roles`: vai trò hệ thống, giữ các role hiện có `USER`, `SELLER`, `ADMIN`.
- `user_roles`: mapping user với role, hiện project đã có.
- `permissions`: danh mục quyền chi tiết.
- `role_permissions`: mapping role với permission.
- `user_permissions`: mapping quyền gán trực tiếp cho từng user.
- `shops`: phạm vi sở hữu dữ liệu của seller.
- Service authorization: kiểm tra cả permission và ownership.

Role permission dùng cho quyền chuẩn của một nhóm người dùng ổn định. User direct permission dùng cho ngoại lệ:

- Cho một nhân viên hỗ trợ thêm quyền `REFUND_READ_ALL`.
- Cho một seller đặc biệt thêm quyền `PROMOTION_CREATE_ALL`.
- Cho một admin phụ thêm quyền `REPORT_EXPORT_ALL`.
- Cho một user nội bộ quyền test feature trước khi mở rộng role.

Rule quyết định:

- Dùng role khi quyền áp dụng cho một nhóm người dùng ổn định.
- Dùng user direct permission khi chỉ vài người cần quyền đặc biệt hoặc quyền tạm thời.
- Direct permission cần có `reason`, `grantedBy`, `grantedAt`, `expiresAt`, `revokedAt`, `active` để audit.
- V1 chỉ hỗ trợ grant thêm quyền theo user; chưa hỗ trợ deny permission override.
- Không nên lạm dụng direct permission vì càng nhiều ngoại lệ càng khó vận hành.

Quy ước đặt tên permission:

- Dùng uppercase snake case.
- Dạng chuẩn: `<RESOURCE>_<ACTION>_<SCOPE>`.
- Scope phổ biến:
  - `SELF`: dữ liệu của chính user.
  - `OWN_SHOP`: dữ liệu thuộc shop của seller hiện tại.
  - `ALL`: dữ liệu toàn hệ thống.
  - Không cần scope nếu quyền là hành động hệ thống hoặc public-like action đã được bảo vệ bằng business rule.

Ví dụ:

- `ORDER_READ_SELF`: customer đọc đơn của chính mình.
- `ORDER_UPDATE_OWN_SHOP`: seller cập nhật đơn thuộc shop mình.
- `ORDER_MANAGE_ALL`: admin quản lý mọi đơn.

Permission entity đề xuất:

| Field | Kiểu | Ghi chú |
|---|---|---|
| `id` | `Long` | Primary key |
| `code` | `String` | Unique, ví dụ `PRODUCT_CREATE` |
| `name` | `String` | Tên hiển thị |
| `description` | `String` | Mô tả quyền |
| `group` | `String` | Nhóm quyền: `USER`, `CATALOG`, `ORDER`, ... |
| `system` | `Boolean` | Quyền hệ thống, không cho xóa tùy tiện |
| `createdAt`, `updatedAt` | `Instant` | Audit cơ bản |

Role-permission mapping:

| Field | Kiểu | Ghi chú |
|---|---|---|
| `role_id` | `Long` | FK tới `roles` |
| `permission_id` | `Long` | FK tới `permissions` |

User direct permission mapping:

| Field | Kiểu | Ghi chú |
|---|---|---|
| `id` | `Long` | Primary key |
| `user_id` | `String` | User được cấp quyền riêng |
| `permission_id` | `Long` | Permission được cấp |
| `reason` | `String` | Lý do cấp quyền |
| `granted_by` | `String` | Admin/user cấp quyền |
| `granted_at` | `Instant` | Thời điểm cấp quyền |
| `expires_at` | `Instant` | Thời điểm hết hạn, có thể null |
| `revoked_at` | `Instant` | Thời điểm thu hồi, có thể null |
| `is_active` | `Boolean` | Trạng thái grant |

Direct permission hợp lệ khi:

- `is_active = true`
- `revoked_at IS NULL`
- `expires_at IS NULL OR expires_at > now`

Permission đề xuất theo module:

| Nhóm | Permission |
|---|---|
| User/Profile | `USER_READ_SELF`, `USER_UPDATE_SELF`, `USER_CHANGE_PASSWORD_SELF`, `USER_DEACTIVATE_SELF`, `USER_READ_ALL`, `USER_LOCK`, `USER_UNLOCK`, `USER_ASSIGN_ROLE`, `USER_PERMISSION_READ`, `USER_PERMISSION_GRANT`, `USER_PERMISSION_REVOKE`, `USER_PERMISSION_MANAGE`, `USER_MANAGE_ALL` |
| Address | `ADDRESS_READ_SELF`, `ADDRESS_CREATE_SELF`, `ADDRESS_UPDATE_SELF`, `ADDRESS_DELETE_SELF`, `ADDRESS_SET_DEFAULT_SELF` |
| Seller/Shop | `SELLER_APPLY`, `SHOP_READ_SELF`, `SHOP_UPDATE_SELF`, `SHOP_READ_ALL`, `SHOP_APPROVE`, `SHOP_REJECT`, `SHOP_LOCK`, `SHOP_UNLOCK`, `SHOP_MANAGE_ALL` |
| Category | `CATEGORY_READ`, `CATEGORY_CREATE`, `CATEGORY_UPDATE`, `CATEGORY_DELETE`, `CATEGORY_REORDER`, `CATEGORY_MANAGE_ALL` |
| Product | `PRODUCT_READ`, `PRODUCT_CREATE_OWN_SHOP`, `PRODUCT_UPDATE_OWN_SHOP`, `PRODUCT_DELETE_OWN_SHOP`, `PRODUCT_SUBMIT_REVIEW_OWN_SHOP`, `PRODUCT_APPROVE`, `PRODUCT_REJECT`, `PRODUCT_MANAGE_ALL` |
| Product Media | `PRODUCT_MEDIA_UPLOAD_OWN_SHOP`, `PRODUCT_MEDIA_UPDATE_OWN_SHOP`, `PRODUCT_MEDIA_DELETE_OWN_SHOP`, `PRODUCT_MEDIA_MANAGE_ALL` |
| Inventory | `INVENTORY_READ_OWN_SHOP`, `INVENTORY_UPDATE_OWN_SHOP`, `INVENTORY_ADJUST_OWN_SHOP`, `INVENTORY_READ_ALL`, `INVENTORY_MANAGE_ALL` |
| Wishlist | `WISHLIST_READ_SELF`, `WISHLIST_UPDATE_SELF` |
| Cart | `CART_READ_SELF`, `CART_UPDATE_SELF`, `CART_CLEAR_SELF` |
| Checkout | `CHECKOUT_CREATE_SELF`, `CHECKOUT_APPLY_COUPON_SELF` |
| Order | `ORDER_READ_SELF`, `ORDER_CANCEL_SELF`, `ORDER_READ_OWN_SHOP`, `ORDER_CONFIRM_OWN_SHOP`, `ORDER_UPDATE_STATUS_OWN_SHOP`, `ORDER_READ_ALL`, `ORDER_UPDATE_STATUS_ALL`, `ORDER_CANCEL_ALL`, `ORDER_MANAGE_ALL` |
| Payment | `PAYMENT_CREATE_SELF`, `PAYMENT_READ_SELF`, `PAYMENT_READ_ALL`, `PAYMENT_CALLBACK_PROCESS`, `PAYMENT_RETRY_SELF`, `PAYMENT_MANAGE_ALL` |
| Refund/Return | `REFUND_CREATE_SELF`, `REFUND_READ_SELF`, `REFUND_READ_OWN_SHOP`, `REFUND_APPROVE_OWN_SHOP`, `REFUND_REJECT_OWN_SHOP`, `REFUND_READ_ALL`, `REFUND_MANAGE_ALL` |
| Shipping | `SHIPPING_READ_SELF`, `SHIPPING_READ_OWN_SHOP`, `SHIPPING_UPDATE_OWN_SHOP`, `SHIPPING_READ_ALL`, `SHIPPING_MANAGE_ALL` |
| Coupon | `COUPON_APPLY_SELF`, `COUPON_CREATE_OWN_SHOP`, `COUPON_UPDATE_OWN_SHOP`, `COUPON_DELETE_OWN_SHOP`, `COUPON_CREATE_ALL`, `COUPON_UPDATE_ALL`, `COUPON_DELETE_ALL`, `COUPON_MANAGE_ALL` |
| Promotion | `PROMOTION_READ`, `PROMOTION_CREATE_OWN_SHOP`, `PROMOTION_UPDATE_OWN_SHOP`, `PROMOTION_DELETE_OWN_SHOP`, `PROMOTION_CREATE_ALL`, `PROMOTION_UPDATE_ALL`, `PROMOTION_DELETE_ALL`, `PROMOTION_MANAGE_ALL` |
| Review | `REVIEW_CREATE_SELF`, `REVIEW_UPDATE_SELF`, `REVIEW_DELETE_SELF`, `REVIEW_REPLY_OWN_SHOP`, `REVIEW_READ_ALL`, `REVIEW_MODERATE`, `REVIEW_MANAGE_ALL` |
| Product Q&A | `QUESTION_CREATE_SELF`, `QUESTION_UPDATE_SELF`, `QUESTION_DELETE_SELF`, `QUESTION_REPLY_OWN_SHOP`, `QUESTION_MODERATE`, `QUESTION_MANAGE_ALL` |
| Notification | `NOTIFICATION_READ_SELF`, `NOTIFICATION_UPDATE_SELF`, `NOTIFICATION_SEND_ALL`, `NOTIFICATION_MANAGE_ALL` |
| Support | `SUPPORT_TICKET_CREATE_SELF`, `SUPPORT_TICKET_READ_SELF`, `SUPPORT_TICKET_READ_OWN_SHOP`, `SUPPORT_TICKET_REPLY_OWN_SHOP`, `SUPPORT_TICKET_READ_ALL`, `SUPPORT_TICKET_MANAGE_ALL` |
| CMS | `CMS_READ`, `CMS_CREATE`, `CMS_UPDATE`, `CMS_DELETE`, `CMS_MANAGE_ALL` |
| Dashboard/Report | `DASHBOARD_READ_SELLER`, `DASHBOARD_READ_ADMIN`, `REPORT_READ_OWN_SHOP`, `REPORT_READ_ALL`, `REPORT_EXPORT_ALL` |
| Audit/System | `AUDIT_LOG_READ`, `SYSTEM_CONFIG_READ`, `SYSTEM_CONFIG_UPDATE`, `SYSTEM_CONFIG_MANAGE`, `ROLE_MANAGE`, `PERMISSION_MANAGE` |

Role mặc định:

- `USER`:
  - Profile/address của chính mình.
  - Wishlist/cart/checkout của chính mình.
  - Đọc/hủy đơn của chính mình theo rule.
  - Tạo payment, đọc payment của chính mình.
  - Tạo refund/review/support ticket theo điều kiện nghiệp vụ.
- `SELLER`:
  - Bao gồm quyền cơ bản của `USER`.
  - Quản lý shop của mình.
  - Tạo/sửa/xóa product, media, variation, inventory thuộc shop mình.
  - Xem và cập nhật đơn thuộc shop mình.
  - Xử lý refund/support/review reply thuộc shop mình.
  - Xem seller dashboard/report của shop mình.
- `ADMIN`:
  - Toàn bộ quyền quản trị hệ thống.
  - Quản lý user, role, permission.
  - Duyệt/từ chối seller/product/review.
  - Quản lý order/payment/refund/shipping toàn hệ thống.
  - Quản lý coupon/promotion toàn sàn, CMS, report, audit, system config.

Yêu cầu kỹ thuật:

- API public không cần permission nhưng vẫn cần filter dữ liệu active/visible.
- API protected cần kiểm tra permission bằng `@PreAuthorize("@authorizationService.hasPermission('PERMISSION_CODE')")` hoặc service-level guard.
- Các nghiệp vụ theo shop phải kiểm tra thêm ownership, không chỉ kiểm tra permission.
- JWT tiếp tục chứa role để tương thích hiện tại; permission được resolve từ database/cache để đổi quyền không cần user đăng nhập lại.
- Permission resolver phải lấy union từ `role_permissions` và `user_permissions` còn hiệu lực.
- Nên cache effective permissions theo user trong thời gian ngắn, nhưng phải clear cache khi role/direct permission thay đổi.
- Seed permission bằng Flyway/Liquibase hoặc seeder idempotent.
- Không cho xóa permission system nếu đang được role sử dụng.
- Mọi thay đổi role permission hoặc direct permission phải ghi audit log.

Ví dụ guard cần có:

- `hasPermission("PRODUCT_UPDATE_OWN_SHOP")` và product phải thuộc shop của seller hiện tại.
- `hasPermission("ORDER_UPDATE_STATUS_OWN_SHOP")` và order phải có item/order group thuộc shop hiện tại.
- `hasPermission("ORDER_MANAGE_ALL")` cho admin quản lý mọi order.
- `hasPermission("REPORT_EXPORT_ALL")` có thể đến từ role `ADMIN` hoặc direct permission cấp riêng cho một user.

API quản lý permission hiện có:

- `GET /admin/permissions`: xem toàn bộ permission hệ thống.
- `GET /admin/roles`: xem role và quyền được gán cho role.
- `GET /admin/roles/{roleCode}/permissions`: xem quyền của một role.
- `PUT /admin/roles/{roleCode}/permissions`: thay thế quyền của một role.
- `GET /admin/users/{userId}/permissions`: xem role permissions, direct permissions và effective permissions của user.
- `POST /admin/users/{userId}/permissions`: grant direct permission cho user, nhận `permissionCode`, `reason`, `expiresAt`.
- `DELETE /admin/users/{userId}/permissions/{permissionCode}`: revoke direct permission đang active của user.

### Seller Và Shop Marketplace

Seller Marketplace MVP hiện đã có:

- Customer/user gửi yêu cầu trở thành seller qua shop application.
- Seller tạo hồ sơ shop: tên shop, slug, logo, mô tả, email, số điện thoại, legal name, tax code optional và địa chỉ lấy hàng.
- Shop có trạng thái: `PENDING`, `APPROVED`, `REJECTED`, `LOCKED`, `INACTIVE`.
- Admin duyệt, từ chối, khóa hoặc mở khóa shop.
- Seller chỉ nên được đăng sản phẩm sau khi shop được duyệt; guard nền đã có qua `ShopOwnershipService`.
- Seller dashboard Phase 3 trả placeholder summary vì product/order chưa có service.

Entity/API hiện có:

- `Shop`.
- `POST /seller/shop/apply`
- `GET /seller/shop`
- `PUT /seller/shop`
- `POST /seller/shop/resubmit`
- `PUT /seller/shop/logo`
- `GET /seller/dashboard`
- `GET /admin/shops`
- `GET /admin/shops/{shopId}`
- `PUT /admin/shops/{shopId}/approve`
- `PUT /admin/shops/{shopId}/reject`
- `PUT /admin/shops/{shopId}/lock`
- `PUT /admin/shops/{shopId}/unlock`

Chưa có trong Phase 3:

- Audit log thật cho approve/reject/lock/unlock.
- Notification/email seller approved/rejected/locked.
- Dashboard doanh thu/sản phẩm/đơn hàng thật.
- Full KYC/document upload.

### Catalog, Search Và Inventory

Catalog là lõi public của marketplace.

Chức năng category:

- Category dạng cây cha-con.
- Category có slug, ảnh, mô tả, display order, active/visible.
- Public xem category tree.
- Admin quản lý category.

Chức năng product:

- Product thuộc một shop/seller.
- Product có SKU gốc, slug, tên, mô tả, brand, material, thumbnail, media gallery.
- Product status: `DRAFT`, `PENDING_REVIEW`, `ACTIVE`, `INACTIVE`, `REJECTED`.
- Admin có thể duyệt/từ chối sản phẩm trước khi public.
- Seller quản lý sản phẩm của shop mình.

Chức năng variation/inventory:

- Variation theo SKU riêng, color, size, additional price, stock.
- Stock nằm ở variation để checkout chính xác.
- Cảnh báo tồn kho thấp cho seller.
- Không cho mua product/variation inactive hoặc hết hàng.

Search/filter/sort:

- Search theo keyword.
- Filter theo category, shop, brand, price range, rating, availability.
- Sort theo newest, price asc/desc, best-selling, rating.
- Pagination chuẩn qua `PageResponse`.

API đề xuất:

- `GET /categories`
- `GET /categories/tree`
- `POST /admin/categories`
- `PUT /admin/categories/{id}`
- `DELETE /admin/categories/{id}`
- `GET /products`
- `GET /products/{slug}`
- `POST /seller/products`
- `PUT /seller/products/{id}`
- `DELETE /seller/products/{id}`
- `POST /seller/products/{id}/media`
- `PUT /seller/products/{id}/variations/{variationId}/stock`
- `PUT /admin/products/{id}/approve`
- `PUT /admin/products/{id}/reject`

### Wishlist Và Recently Viewed

Tính năng hỗ trợ trải nghiệm mua sắm:

- Customer thêm/xóa sản phẩm khỏi wishlist.
- Xem danh sách wishlist có phân trang.
- Lưu recently viewed products theo user hoặc session.
- Public product detail trả thêm trạng thái `wished` nếu user đã đăng nhập.

API đề xuất:

- `GET /users/me/wishlist`
- `POST /users/me/wishlist/{productId}`
- `DELETE /users/me/wishlist/{productId}`
- `GET /users/me/recently-viewed`

### Address, Cart, Checkout Và Order

Address:

- Customer có nhiều địa chỉ.
- Có một địa chỉ mặc định.
- Order phải lưu snapshot địa chỉ giao hàng để lịch sử đơn không đổi khi user sửa address.

API đề xuất:

- `GET /users/me/addresses`
- `POST /users/me/addresses`
- `PUT /users/me/addresses/{id}`
- `DELETE /users/me/addresses/{id}`
- `PUT /users/me/addresses/{id}/default`

Cart:

- Mỗi customer có một cart active.
- Cart có thể chứa sản phẩm từ nhiều seller.
- Thêm sản phẩm trùng variation thì tăng quantity.
- Mỗi lần đọc hoặc cập nhật cart phải validate product active, variation active và stock.

API đề xuất:

- `GET /cart`
- `POST /cart/items`
- `PUT /cart/items/{id}`
- `DELETE /cart/items/{id}`
- `DELETE /cart/items`

Checkout:

- Validate cart không rỗng.
- Validate địa chỉ giao hàng.
- Validate stock lần cuối.
- Áp dụng coupon/promotion.
- Tách order theo seller nếu cart có sản phẩm từ nhiều shop.
- Lưu snapshot product name, SKU, variation, price, thumbnail, shipping address.
- Clear cart item đã checkout sau khi tạo order/payment thành công theo flow.

Order status đề xuất:

- `PENDING_PAYMENT`: đã tạo order nhưng chưa thanh toán.
- `PAID`: thanh toán online thành công.
- `CONFIRMED`: seller/admin xác nhận đơn.
- `PACKING`: seller đang chuẩn bị hàng.
- `SHIPPING`: đang giao hàng.
- `COMPLETED`: giao thành công.
- `CANCELLED`: đơn đã hủy.
- `REFUND_REQUESTED`: customer yêu cầu hoàn tiền/đổi trả.
- `REFUNDED`: đã hoàn tiền.

API order đề xuất:

- `POST /checkout`
- `GET /orders`
- `GET /orders/{id}`
- `POST /orders/{id}/cancel`
- `GET /seller/orders`
- `PUT /seller/orders/{id}/confirm`
- `PUT /seller/orders/{id}/packing`
- `PUT /seller/orders/{id}/shipping`
- `PUT /seller/orders/{id}/completed`
- `GET /admin/orders`
- `PUT /admin/orders/{id}/status`

### Payment, VNPay, Momo Và Refund

Payment v1 cần hỗ trợ:

- COD để test end-to-end nhanh.
- VNPay.
- Momo.

Yêu cầu payment:

- Tạo payment transaction khi checkout.
- Với VNPay/Momo, backend tạo payment URL và trả về frontend.
- Callback/webhook phải validate signature.
- Callback/webhook phải idempotent để không xử lý trùng.
- Lưu `provider`, `transactionId`, `amount`, `status`, `rawResponse`, `paidAt`.
- Sync payment status với order status.
- Không tin dữ liệu amount/orderId từ callback nếu chưa đối chiếu với database.

Payment status đề xuất:

- `PENDING`
- `SUCCESS`
- `FAILED`
- `CANCELLED`
- `EXPIRED`
- `REFUNDED`

API đề xuất:

- `POST /payments/{orderId}/create`
- `GET /payments/{paymentId}`
- `POST /payments/vnpay/callback`
- `POST /payments/momo/callback`
- `POST /payments/{paymentId}/retry`

Refund/return:

- Customer tạo yêu cầu refund/return cho order đủ điều kiện.
- Seller/Admin duyệt hoặc từ chối.
- Lưu lý do, bằng chứng, số tiền, trạng thái xử lý.
- Với online payment, refund cần liên kết với provider transaction.

API đề xuất:

- `POST /orders/{orderId}/refunds`
- `GET /orders/{orderId}/refunds`
- `PUT /seller/refunds/{id}/approve`
- `PUT /seller/refunds/{id}/reject`
- `PUT /admin/refunds/{id}/status`

### Shipping, Return Và Dispute

Shipping v1:

- Shipping fee có thể tính fixed hoặc rule-based theo shop/order amount.
- Order lưu tracking code, carrier, shipping status.
- Shipping address phải là snapshot.
- Chuẩn bị abstraction để sau tích hợp GHN, GHTK, Viettel Post.

Shipping status đề xuất:

- `NOT_CREATED`
- `READY_TO_SHIP`
- `PICKED_UP`
- `IN_TRANSIT`
- `DELIVERED`
- `FAILED_DELIVERY`
- `RETURNING`
- `RETURNED`

Dispute/support cho đơn hàng:

- Customer mở ticket nếu có vấn đề với đơn.
- Seller phản hồi ticket thuộc shop mình.
- Admin can thiệp khi tranh chấp không giải quyết được.

### Coupon, Promotion, Flash Sale Và Loyalty

Coupon:

- Coupon toàn sàn do admin tạo.
- Coupon theo seller/shop.
- Rule: thời gian hiệu lực, min order amount, max discount, usage limit, usage per user.
- Coupon usage chỉ ghi nhận khi order tạo thành công.

Promotion:

- Promotion theo product/category/shop.
- Loại giảm giá: `PERCENT`, `FIXED`.
- Có thể mở rộng thêm bundle/combo sau.

Flash sale:

- Campaign có thời gian bắt đầu/kết thúc.
- Giới hạn số lượng bán.
- Giá flash sale phải được snapshot vào order item.

Free shipping voucher:

- Áp dụng theo shop hoặc toàn sàn.
- Có min order amount và max shipping discount.

Loyalty point v2:

- Tích điểm khi order `COMPLETED`.
- Dùng điểm để giảm giá.
- Lưu lịch sử cộng/trừ điểm.

API đề xuất:

- `POST /coupons/validate`
- `POST /admin/coupons`
- `POST /seller/coupons`
- `POST /admin/promotions`
- `POST /seller/promotions`
- `GET /promotions/active`

### Review, Rating Và Product Q&A

Review:

- Chỉ customer đã mua hàng và order `COMPLETED` mới được review.
- Một order item chỉ được review một lần.
- Review có rating 1-5, comment, ảnh.
- Seller có thể reply review.
- Admin có thể ẩn hoặc duyệt review.

Rating:

- Product detail trả average rating và review count.
- Có thể cache rating summary để tối ưu.

Product Q&A:

- Customer đặt câu hỏi về sản phẩm.
- Seller trả lời câu hỏi thuộc sản phẩm shop mình.
- Admin moderate nội dung vi phạm.

API đề xuất:

- `GET /products/{id}/reviews`
- `POST /orders/{orderId}/items/{itemId}/reviews`
- `PUT /reviews/{id}`
- `DELETE /reviews/{id}`
- `POST /seller/reviews/{id}/reply`
- `PUT /admin/reviews/{id}/approve`
- `PUT /admin/reviews/{id}/hide`
- `GET /products/{id}/questions`
- `POST /products/{id}/questions`
- `POST /seller/questions/{id}/answers`

### Notification, Email Và Support

Notification center:

- Lưu notification trong database.
- User đọc/chưa đọc notification.
- Gửi notification khi order đổi trạng thái, payment thành công/thất bại, refund update, seller/product được duyệt/từ chối.

Email template cần bổ sung:

- Order confirmation.
- Payment success/failure.
- Shipping update.
- Refund update.
- Seller approved/rejected.
- Product approved/rejected.

Support ticket:

- Customer tạo ticket.
- Seller hoặc admin phản hồi.
- Ticket status: `OPEN`, `PENDING`, `RESOLVED`, `CLOSED`.

API đề xuất:

- `GET /notifications`
- `PUT /notifications/{id}/read`
- `PUT /notifications/read-all`
- `POST /support/tickets`
- `GET /support/tickets`
- `POST /support/tickets/{id}/messages`

### Admin CMS, Report Và Operation

CMS:

- Banner trang chủ.
- Featured category.
- Featured product.
- Campaign section.
- Cấu hình nội dung trang chủ không cần deploy lại backend.

Admin dashboard:

- GMV.
- Revenue.
- Order count.
- Active users.
- Active sellers.
- Top products.
- Top sellers.
- Payment success/fail rate.
- Refund rate.

Seller dashboard:

- Doanh thu theo ngày/tháng.
- Số đơn theo trạng thái.
- Sản phẩm bán chạy.
- Tồn kho thấp.
- Tỷ lệ hủy/hoàn.

Seller payout v2:

- Commission rate.
- Seller balance.
- Payout request.
- Payout history.
- Đối soát payout với order completed/refunded.

System config:

- Commission mặc định.
- Shipping fee rule.
- Maintenance mode.
- Feature flags.
- Upload limit.

Audit log:

- Admin login.
- Đổi role/permission.
- Duyệt/từ chối seller.
- Duyệt/từ chối product.
- Sửa giá/sửa tồn kho.
- Đổi trạng thái order.
- Refund approval.
- Payment manual action.

### Security, Quality Và Production Readiness

Database:

- Thêm Flyway hoặc Liquibase.
- Không dùng `ddl-auto=update` cho production.
- Migration phải version rõ ràng.
- Seed role/permission bằng migration hoặc startup seeder có kiểm soát.

Security:

- Rate limit cho auth, OTP, resend OTP, forgot password, payment callback.
- Idempotency key cho checkout/payment/refund.
- Correlation ID cho request tracing.
- Không log access token, refresh token, OTP, password hoặc secret.
- Validate ownership ở service layer.
- Soft delete với dữ liệu nghiệp vụ quan trọng.

Upload:

- Giữ validate MIME, size và magic bytes hiện có.
- Mở rộng Cloudinary cho product/review/support evidence.
- Có cleanup policy cho media cũ hoặc media orphan.

Observability:

- Health check.
- Structured logs.
- Metrics cho auth, order, payment, email, upload.
- Slow query tracking.
- Alert khi payment callback lỗi, email fail nhiều, order stuck.

Backup/restore:

- Backup database định kỳ.
- Tài liệu restore local/staging.
- Không xóa cứng order/payment/audit.

## Kế Hoạch Triển Khai Chuẩn

Kế hoạch triển khai nên đi theo hướng tăng trưởng có kiểm soát: mỗi phase phải tạo ra một năng lực chạy được, có test, có API rõ ràng và không phá flow auth hiện tại.

### Nguyên Tắc Triển Khai

- Mỗi domain phải có đủ lớp: entity, repository, service, DTO, mapper, controller, error code và test.
- Không đưa logic nghiệp vụ vào controller.
- Không trả entity trực tiếp ra API; luôn dùng response DTO.
- Các API list phải có pagination, sort và filter hợp lý.
- Các nghiệp vụ tiền, stock, order, payment phải chạy trong transaction.
- Các trạng thái quan trọng phải được định nghĩa bằng enum và kiểm soát transition.
- Dữ liệu lịch sử như order item, payment, refund, audit phải lưu snapshot cần thiết.
- Với marketplace, mọi API seller phải có ownership guard.
- Với admin action, phải có audit log.
- Trước khi thêm online payment, COD flow phải chạy ổn định end-to-end.

### Chuẩn Hoàn Thành Cho Mỗi Module

Một module chỉ được xem là hoàn thành khi có đủ:

- API public/protected/admin hoặc seller đúng phạm vi.
- Validation input rõ ràng.
- Error code riêng cho lỗi nghiệp vụ chính.
- Permission guard đúng role và ownership.
- Unit test cho service logic.
- Integration test cho flow chính.
- Swagger/OpenAPI mô tả request/response.
- Postman collection hoặc sample request nếu là flow quan trọng.
- README cập nhật nếu module có biến môi trường hoặc flow vận hành mới.

### Thứ Tự Phụ Thuộc Kỹ Thuật

Không nên làm module theo cảm tính. Thứ tự phụ thuộc nên là:

1. Migration và config nền.
2. Permission/RBAC.
3. Seller/shop ownership.
4. Catalog/product/inventory.
5. Address/cart.
6. Checkout/order.
7. Payment.
8. Shipping/refund.
9. Promotion/review.
10. Notification/support/dashboard/operation.

Lý do:

- Permission phải có trước để các API mới không phải sửa lại bảo mật nhiều lần.
- Shop phải có trước product để product có owner rõ ràng.
- Product/inventory phải có trước cart/checkout.
- Checkout/order phải ổn định trước khi tích hợp VNPay/Momo.
- Refund/shipping/review đều phụ thuộc order lifecycle.

## Phase Roadmap

### Phase 1: Foundation - Done

- README/PLAN được giữ ở UTF-8; nếu terminal hiển thị lỗi chữ thì chỉnh encoding terminal/editor.
- Đã thêm Flyway migration `V1__init_schema.sql`.
- Đã chuẩn hóa config `local`, `dev`, `prod`, `test`.
- Đã tách DB/JWT/mail/Cloudinary/admin seed secret qua environment variable.
- Đã bổ sung integration test cho migration, auth/profile/session.
- Đã bổ sung test cho token rotation, refresh token reuse và protected endpoint.

Kết quả mong muốn:

- Project có migration versioning.
- Auth hiện tại có test tin cậy hơn ở cả service unit test và HTTP integration test.
- Config production không phụ thuộc `ddl-auto=update`.

### Phase 2: RBAC Permission - Done

- Đã thêm `Permission` entity.
- Đã thêm `RolePermission` mapping qua `role_permissions`.
- Đã thêm direct user permission qua `UserPermission` và `user_permissions`.
- Đã seed permission mặc định cho `USER`, `SELLER`, `ADMIN`.
- Đã thêm repository/service cho role, permission và user direct permission.
- Đã bổ sung permission resolver để lấy effective permissions từ role permissions + active direct permissions.
- Đã áp dụng `@PreAuthorize` cho admin permission APIs.
- Đã thêm admin APIs để xem/cập nhật role permissions và grant/revoke direct user permission.
- Đã có unit test cho permission seed, role permission, direct user permission và authorization resolver.
- Còn lại: audit log chi tiết và cache invalidation.

Kết quả mong muốn:

- Có phân quyền chi tiết thay vì chỉ kiểm tra role.
- Permission được seed idempotent, chạy lại không tạo trùng dữ liệu.
- Admin có API để quản lý role permissions và direct user permissions.
- Effective permissions được resolve từ database, không phụ thuộc JWT permission claim.
- Audit log chi tiết và cache invalidation sẽ được xử lý ở module vận hành/production readiness.

### Phase 3: Seller Marketplace - Done

- Đã thêm Flyway migration `V2__seller_marketplace.sql`.
- Đã thêm `Shop` entity, `ShopRepository`, DTO, mapper, service và error code.
- Đã thêm seller APIs cho apply, read/update shop, resubmit, upload logo và dashboard placeholder.
- Đã thêm admin APIs cho list/detail shop, approve, reject, lock và unlock.
- Đã thêm `ShopOwnershipService` để lấy shop hiện tại, kiểm tra owner và yêu cầu shop đã `APPROVED`.
- Đã bổ sung `SELLER_APPLY` cho role `USER`; user chưa là seller vẫn có thể đăng ký shop và chỉnh hồ sơ pending/rejected.
- Khi admin approve shop, owner được gán role `SELLER`.
- Đã có unit test cho seller flow và admin moderation flow.

Kết quả mong muốn:

- User có thể đăng ký làm seller.
- Admin có thể duyệt/từ chối/khóa/mở khóa seller shop.
- Seller có shop riêng và có guard nền để các API Phase 4+ chỉ thao tác trong phạm vi shop.

Còn lại để làm ở phase sau:

- Audit log thật cho thao tác approve/reject/lock/unlock.
- Notification/email khi shop được duyệt/từ chối/khóa.
- Dashboard doanh thu/sản phẩm/đơn hàng thật sau khi có catalog/order.
- Full KYC/document upload.

### Phase 4: Catalog - Done

- Đã thêm Flyway migration `V3__catalog_phase4.sql`.
- Đã thêm repository/service/controller cho category.
- Đã thêm product CRUD cho seller gắn với shop đã `APPROVED`.
- Đã thêm variation/inventory CRUD, stock product được tổng hợp từ variation active.
- Đã thêm product media upload qua Cloudinary, primary media cập nhật thumbnail product.
- Đã thêm product moderation cho admin: approve/reject product trước khi public.
- Đã thêm public category list/tree và product search/filter/sort/pagination.

Kết quả mong muốn:

- Guest/customer xem được catalog public.
- Seller đăng sản phẩm và quản lý tồn kho.
- Admin kiểm duyệt được sản phẩm.

Còn lại để làm ở phase sau:

- Wishlist và recently viewed.
- Audit log thật cho approve/reject product.
- Notification/email khi product được duyệt/từ chối.
- Media cleanup policy cho product media cũ/orphan.

### Phase 5: Address, Cart, Checkout Và Payment v1 - Done

- Đã thêm Flyway migration `V4__phase5_checkout_payment.sql`.
- Đã thêm address API cho address book, default address và soft delete.
- Đã thêm cart API cho active cart, add/update/remove/clear item và merge variation trùng.
- Đã thêm checkout selected cart item, validate stock, lock variation, split order theo seller/shop.
- Đã snapshot product/address/pricing vào order item/order.
- Đã tạo payment group gom nhiều order trong một lần checkout.
- Đã hỗ trợ payment v1 gồm `COD`, `VNPAY`, `MOMO` với payment URL, callback verify signature và idempotency.
- Đã thêm scheduled job expire online payment pending quá TTL và hoàn stock.

Kết quả mong muốn:

- Customer có thể tạo đơn từ cart.
- Cart nhiều seller được tách thành nhiều order phù hợp.
- Order không bị thay đổi lịch sử khi product/address thay đổi.

Còn lại để làm ở phase sau:

- Order history/detail/cancel cho customer.
- Seller/admin order management và shipping lifecycle.
- Promotion/coupon thật, shipping fee thật và refund flow.

### Phase 6: Payment Provider Hardening

- Bổ sung test tích hợp thực tế với sandbox VNPay/MoMo khi có credential thật.
- Chuẩn hóa provider response theo yêu cầu production của từng cổng.
- Bổ sung reconciliation/check transaction API nếu provider hỗ trợ.
- Bổ sung monitoring/alert cho callback lỗi hoặc payment stuck.

Kết quả mong muốn:

- Payment online ổn định hơn trước khi đưa production.
- Có quy trình đối soát khi callback bị thiếu hoặc trễ.

### Phase 7: Order Và Shipping

- Customer xem lịch sử và chi tiết order.
- Customer hủy order khi còn được phép.
- Seller quản lý order của shop.
- Admin quản lý mọi order.
- Thêm shipping status, carrier, tracking code.
- Chuẩn bị adapter cho GHN/GHTK/Viettel Post.

Kết quả mong muốn:

- Có order lifecycle rõ ràng.
- Seller xử lý đơn theo trạng thái.
- Customer theo dõi được đơn.

### Phase 8: Promotion Và Review

- Coupon toàn sàn.
- Coupon theo seller.
- Promotion theo product/category/shop.
- Flash sale.
- Free shipping voucher.
- Review/rating sau khi order completed.
- Seller reply review.
- Admin moderate review.
- Product Q&A.

Kết quả mong muốn:

- Có discount engine đủ dùng cho marketplace.
- Review chỉ đến từ giao dịch thật.
- Product detail có rating/review/Q&A.

### Phase 9: Refund, Support Và Notification

- Refund/return request.
- Seller/Admin approve/reject refund.
- Support ticket cho order dispute.
- Notification center.
- Email template cho order/payment/shipping/refund/seller/product moderation.

Kết quả mong muốn:

- Customer có kênh xử lý vấn đề sau mua.
- Seller/admin có quy trình phản hồi.
- User nhận được thông báo theo sự kiện quan trọng.

### Phase 10: Dashboard Và Production

- Admin dashboard.
- Seller dashboard nâng cao.
- Audit log.
- Reports.
- CMS banner/homepage.
- Seller payout v2.
- Metrics/logging/health check.
- Backup/restore checklist.
- Rate limit và monitoring cho endpoint nhạy cảm.

Kết quả mong muốn:

- Hệ thống đủ công cụ vận hành.
- Có dữ liệu báo cáo cho admin/seller.
- Sẵn sàng triển khai staging/production có kiểm soát.

## Test Checklist Cho Roadmap

- Auth/session/token integration test.
- OTP register/reset password test.
- Refresh token rotation/reuse detection test.
- Permission test theo role.
- Direct user permission test.
- Effective permission union test.
- Grant/revoke direct permission test.
- Expired/revoked direct permission không còn hiệu lực.
- Seller ownership test.
- Category/product CRUD test.
- Product search/filter/sort/pagination test.
- Product media upload validation test.
- Cart add/update/remove/clear test.
- Checkout stock validation test.
- Multi-seller checkout split order test.
- Pricing snapshot test.
- Coupon/promotion validation test.
- VNPay/Momo callback success test.
- VNPay/Momo callback failed test.
- VNPay/Momo invalid signature test.
- VNPay/Momo duplicate callback idempotency test.
- Order status transition test.
- Customer cancel order test.
- Seller/admin order update permission test.
- Refund/return request test.
- Review only after completed order test.
- Review one time per order item test.
- Admin moderation test.
- Notification read/unread test.
- Support ticket flow test.
- Audit log creation test.
- Upload security test.

## Ưu Tiên Triển Khai Gần Nhất

Thứ tự nên làm ngay sau nền auth/user hiện tại:

1. Làm category/product/variation/media gắn với shop đã được approve.
2. Làm address/cart/checkout/order.
3. Làm COD trước, sau đó VNPay/Momo.
4. Làm promotion/review/refund/dashboard sau khi order flow ổn định.
5. Bổ sung audit log, notification/email và dashboard thật khi các domain nghiệp vụ đã có dữ liệu.
