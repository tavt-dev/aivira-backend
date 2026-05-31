CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    permission_group VARCHAR(50) NOT NULL,
    is_system BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_permissions_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id VARCHAR(255) NOT NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password VARCHAR(255),
    provider VARCHAR(255),
    provider_user_id VARCHAR(128),
    email_verified BIT(1) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    gender VARCHAR(255),
    phone_number VARCHAR(15),
    avatar_url VARCHAR(255),
    avatar_public_id VARCHAR(255),
    is_active BIT(1) NOT NULL,
    is_locked BIT(1),
    is_deleted BIT(1),
    token_version INT NOT NULL,
    failed_login_attempts INT NOT NULL,
    first_failed_login_at DATETIME(6),
    lockout_until DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id VARCHAR(255) NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(255) NOT NULL,
    permission_id BIGINT NOT NULL,
    reason VARCHAR(500),
    expires_at DATETIME(6),
    granted_by VARCHAR(255),
    granted_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    is_active BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_permission_active UNIQUE (user_id, permission_id, is_active),
    CONSTRAINT fk_user_permissions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id),
    CONSTRAINT fk_user_permissions_granted_by FOREIGN KEY (granted_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE addresses (
    address_id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    address_line VARCHAR(500) NOT NULL,
    ward VARCHAR(255),
    district VARCHAR(255),
    city VARCHAR(255),
    is_default BIT(1),
    user_id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (address_id),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_otp (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(255) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    otp_type VARCHAR(30) NOT NULL,
    expires_time DATETIME(6) NOT NULL,
    used_at DATETIME(6),
    used BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_user_otp_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invalidated_tokens (
    id VARCHAR(255) NOT NULL,
    expiry_time DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    jti VARCHAR(36) NOT NULL,
    family_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    replaced_by VARCHAR(36),
    last_used_at DATETIME(6),
    device_info VARCHAR(512),
    ip_address VARCHAR(45),
    revoked BIT(1) NOT NULL,
    revoked_at DATETIME(6),
    revocation_reason VARCHAR(100),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_id UNIQUE (id),
    CONSTRAINT uk_refresh_tokens_jti UNIQUE (jti),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(150) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    image_url VARCHAR(255),
    image_public_id VARCHAR(255),
    display_order INT NOT NULL,
    parent_id BIGINT,
    is_active BIT(1) NOT NULL,
    is_visible BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_category_name UNIQUE (category_name),
    CONSTRAINT uk_categories_slug UNIQUE (slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    slug VARCHAR(255),
    description LONGTEXT NOT NULL,
    brand VARCHAR(100),
    material VARCHAR(100),
    thumbnail_url VARCHAR(255),
    thumbnail_public_id VARCHAR(255),
    price DECIMAL(19, 2) NOT NULL,
    original_price DECIMAL(19, 2),
    discount_percentage DECIMAL(5, 2),
    weight DECIMAL(10, 2),
    stock_quantity INT NOT NULL,
    sold_count INT NOT NULL,
    is_active BIT(1),
    is_featured BIT(1),
    category_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_variations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku VARCHAR(50) NOT NULL,
    color VARCHAR(50) NOT NULL,
    size VARCHAR(50) NOT NULL,
    additional_price DECIMAL(19, 2) NOT NULL,
    stock_quantity INT NOT NULL,
    image_url VARCHAR(255),
    image_public_id VARCHAR(255),
    is_active BIT(1) NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_variations_sku UNIQUE (sku),
    CONSTRAINT fk_product_variations_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_media (
    id BIGINT NOT NULL AUTO_INCREMENT,
    media_url VARCHAR(1000) NOT NULL,
    media_public_id VARCHAR(255) NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    alt_text VARCHAR(255),
    sort_order INT NOT NULL,
    is_primary BIT(1) NOT NULL,
    is_active BIT(1) NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_media_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE carts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(255) NOT NULL,
    is_active BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    product_variation_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id),
    CONSTRAINT fk_cart_items_product_variation FOREIGN KEY (product_variation_id) REFERENCES product_variations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_code VARCHAR(50) NOT NULL,
    subtotal DECIMAL(19, 2) NOT NULL,
    shipping_fee DECIMAL(19, 2) NOT NULL,
    discount_amount DECIMAL(19, 2) NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    coupon_code VARCHAR(50),
    notes VARCHAR(500),
    cancel_reason VARCHAR(500),
    order_status VARCHAR(50) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    address_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_order_code UNIQUE (order_code),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_address FOREIGN KEY (address_id) REFERENCES addresses (address_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_variation_id BIGINT,
    product_name VARCHAR(255) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    variation_color VARCHAR(50),
    variation_size VARCHAR(50),
    thumbnail_url VARCHAR(255),
    base_price DECIMAL(19, 2) NOT NULL,
    additional_price DECIMAL(19, 2),
    discount_amount DECIMAL(19, 2),
    final_price DECIMAL(19, 2) NOT NULL,
    promotion_name VARCHAR(255),
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    transaction_id VARCHAR(100),
    provider_response TEXT,
    paid_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE coupons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    type VARCHAR(255) NOT NULL,
    value DECIMAL(19, 2) NOT NULL,
    max_discount_amount DECIMAL(19, 2),
    min_order_amount DECIMAL(19, 2),
    usage_limit INT,
    usage_limit_per_user INT,
    used_count INT NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_coupons_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE coupon_usages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    discount_amount DECIMAL(19, 2) NOT NULL,
    coupon_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    order_id BIGINT NOT NULL,
    used_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_coupon_usages_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT fk_coupon_usages_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_coupon_usages_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE promotions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    promotion_name VARCHAR(150) NOT NULL,
    description LONGTEXT NOT NULL,
    promotion_type VARCHAR(255) NOT NULL,
    value DECIMAL(19, 2) NOT NULL,
    max_discount_amount DECIMAL(19, 2),
    promotion_scope VARCHAR(255) NOT NULL,
    target_id BIGINT NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    is_active BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_promotions_promotion_name UNIQUE (promotion_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rating INT NOT NULL,
    comment TEXT,
    is_approved BIT(1) NOT NULL,
    admin_reply TEXT,
    user_id VARCHAR(255) NOT NULL,
    product_id BIGINT NOT NULL,
    product_variation_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_reviews_product_variation FOREIGN KEY (product_variation_id) REFERENCES product_variations (id),
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE review_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    image_url VARCHAR(500) NOT NULL,
    image_public_id VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    review_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_review_images_review FOREIGN KEY (review_id) REFERENCES reviews (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (code, description) VALUES
    ('USER', 'USER ROLE'),
    ('ADMIN', 'ADMIN ROLE');

INSERT INTO permissions (code, name, description, permission_group, is_system, created_at, updated_at) VALUES
    ('USER_READ_SELF', 'User read self', 'System permission USER_READ_SELF', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_UPDATE_SELF', 'User update self', 'System permission USER_UPDATE_SELF', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_CHANGE_PASSWORD_SELF', 'User change password self', 'System permission USER_CHANGE_PASSWORD_SELF', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_DEACTIVATE_SELF', 'User deactivate self', 'System permission USER_DEACTIVATE_SELF', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_READ_ALL', 'User read all', 'System permission USER_READ_ALL', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_LOCK', 'User lock', 'System permission USER_LOCK', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_UNLOCK', 'User unlock', 'System permission USER_UNLOCK', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_ASSIGN_ROLE', 'User assign role', 'System permission USER_ASSIGN_ROLE', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_PERMISSION_READ', 'User permission read', 'System permission USER_PERMISSION_READ', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_PERMISSION_GRANT', 'User permission grant', 'System permission USER_PERMISSION_GRANT', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_PERMISSION_REVOKE', 'User permission revoke', 'System permission USER_PERMISSION_REVOKE', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_PERMISSION_MANAGE', 'User permission manage', 'System permission USER_PERMISSION_MANAGE', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_MANAGE_ALL', 'User manage all', 'System permission USER_MANAGE_ALL', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ADDRESS_READ_SELF', 'Address read self', 'System permission ADDRESS_READ_SELF', 'ADDRESS', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ADDRESS_CREATE_SELF', 'Address create self', 'System permission ADDRESS_CREATE_SELF', 'ADDRESS', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ADDRESS_UPDATE_SELF', 'Address update self', 'System permission ADDRESS_UPDATE_SELF', 'ADDRESS', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ADDRESS_DELETE_SELF', 'Address delete self', 'System permission ADDRESS_DELETE_SELF', 'ADDRESS', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ADDRESS_SET_DEFAULT_SELF', 'Address set default self', 'System permission ADDRESS_SET_DEFAULT_SELF', 'ADDRESS', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CATEGORY_READ', 'Category read', 'System permission CATEGORY_READ', 'CATEGORY', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CATEGORY_CREATE', 'Category create', 'System permission CATEGORY_CREATE', 'CATEGORY', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CATEGORY_UPDATE', 'Category update', 'System permission CATEGORY_UPDATE', 'CATEGORY', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CATEGORY_DELETE', 'Category delete', 'System permission CATEGORY_DELETE', 'CATEGORY', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CATEGORY_REORDER', 'Category reorder', 'System permission CATEGORY_REORDER', 'CATEGORY', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CATEGORY_MANAGE_ALL', 'Category manage all', 'System permission CATEGORY_MANAGE_ALL', 'CATEGORY', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PRODUCT_READ', 'Product read', 'System permission PRODUCT_READ', 'PRODUCT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PRODUCT_MANAGE_ALL', 'Product manage all', 'System permission PRODUCT_MANAGE_ALL', 'PRODUCT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PRODUCT_MEDIA_MANAGE_ALL', 'Product media manage all', 'System permission PRODUCT_MEDIA_MANAGE_ALL', 'PRODUCT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('INVENTORY_READ_ALL', 'Inventory read all', 'System permission INVENTORY_READ_ALL', 'INVENTORY', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('INVENTORY_MANAGE_ALL', 'Inventory manage all', 'System permission INVENTORY_MANAGE_ALL', 'INVENTORY', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('WISHLIST_READ_SELF', 'Wishlist read self', 'System permission WISHLIST_READ_SELF', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('WISHLIST_UPDATE_SELF', 'Wishlist update self', 'System permission WISHLIST_UPDATE_SELF', 'USER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CART_READ_SELF', 'Cart read self', 'System permission CART_READ_SELF', 'CART', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CART_UPDATE_SELF', 'Cart update self', 'System permission CART_UPDATE_SELF', 'CART', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CART_CLEAR_SELF', 'Cart clear self', 'System permission CART_CLEAR_SELF', 'CART', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CHECKOUT_CREATE_SELF', 'Checkout create self', 'System permission CHECKOUT_CREATE_SELF', 'CHECKOUT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CHECKOUT_APPLY_COUPON_SELF', 'Checkout apply coupon self', 'System permission CHECKOUT_APPLY_COUPON_SELF', 'CHECKOUT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ORDER_READ_SELF', 'Order read self', 'System permission ORDER_READ_SELF', 'ORDER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ORDER_CANCEL_SELF', 'Order cancel self', 'System permission ORDER_CANCEL_SELF', 'ORDER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ORDER_READ_ALL', 'Order read all', 'System permission ORDER_READ_ALL', 'ORDER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ORDER_UPDATE_STATUS_ALL', 'Order update status all', 'System permission ORDER_UPDATE_STATUS_ALL', 'ORDER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ORDER_CANCEL_ALL', 'Order cancel all', 'System permission ORDER_CANCEL_ALL', 'ORDER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ORDER_MANAGE_ALL', 'Order manage all', 'System permission ORDER_MANAGE_ALL', 'ORDER', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PAYMENT_CREATE_SELF', 'Payment create self', 'System permission PAYMENT_CREATE_SELF', 'PAYMENT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PAYMENT_READ_SELF', 'Payment read self', 'System permission PAYMENT_READ_SELF', 'PAYMENT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PAYMENT_READ_ALL', 'Payment read all', 'System permission PAYMENT_READ_ALL', 'PAYMENT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PAYMENT_CALLBACK_PROCESS', 'Payment callback process', 'System permission PAYMENT_CALLBACK_PROCESS', 'PAYMENT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PAYMENT_RETRY_SELF', 'Payment retry self', 'System permission PAYMENT_RETRY_SELF', 'PAYMENT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PAYMENT_MANAGE_ALL', 'Payment manage all', 'System permission PAYMENT_MANAGE_ALL', 'PAYMENT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REFUND_CREATE_SELF', 'Refund create self', 'System permission REFUND_CREATE_SELF', 'REFUND', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REFUND_READ_SELF', 'Refund read self', 'System permission REFUND_READ_SELF', 'REFUND', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REFUND_READ_ALL', 'Refund read all', 'System permission REFUND_READ_ALL', 'REFUND', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REFUND_MANAGE_ALL', 'Refund manage all', 'System permission REFUND_MANAGE_ALL', 'REFUND', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('SHIPPING_READ_SELF', 'Shipping read self', 'System permission SHIPPING_READ_SELF', 'SHIPPING', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('SHIPPING_READ_ALL', 'Shipping read all', 'System permission SHIPPING_READ_ALL', 'SHIPPING', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('SHIPPING_MANAGE_ALL', 'Shipping manage all', 'System permission SHIPPING_MANAGE_ALL', 'SHIPPING', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('COUPON_APPLY_SELF', 'Coupon apply self', 'System permission COUPON_APPLY_SELF', 'COUPON', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('COUPON_CREATE_ALL', 'Coupon create all', 'System permission COUPON_CREATE_ALL', 'COUPON', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('COUPON_UPDATE_ALL', 'Coupon update all', 'System permission COUPON_UPDATE_ALL', 'COUPON', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('COUPON_DELETE_ALL', 'Coupon delete all', 'System permission COUPON_DELETE_ALL', 'COUPON', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('COUPON_MANAGE_ALL', 'Coupon manage all', 'System permission COUPON_MANAGE_ALL', 'COUPON', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PROMOTION_READ', 'Promotion read', 'System permission PROMOTION_READ', 'PROMOTION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PROMOTION_CREATE_ALL', 'Promotion create all', 'System permission PROMOTION_CREATE_ALL', 'PROMOTION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PROMOTION_UPDATE_ALL', 'Promotion update all', 'System permission PROMOTION_UPDATE_ALL', 'PROMOTION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PROMOTION_DELETE_ALL', 'Promotion delete all', 'System permission PROMOTION_DELETE_ALL', 'PROMOTION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PROMOTION_MANAGE_ALL', 'Promotion manage all', 'System permission PROMOTION_MANAGE_ALL', 'PROMOTION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REVIEW_CREATE_SELF', 'Review create self', 'System permission REVIEW_CREATE_SELF', 'REVIEW', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REVIEW_UPDATE_SELF', 'Review update self', 'System permission REVIEW_UPDATE_SELF', 'REVIEW', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REVIEW_DELETE_SELF', 'Review delete self', 'System permission REVIEW_DELETE_SELF', 'REVIEW', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REVIEW_READ_ALL', 'Review read all', 'System permission REVIEW_READ_ALL', 'REVIEW', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REVIEW_MODERATE', 'Review moderate', 'System permission REVIEW_MODERATE', 'REVIEW', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REVIEW_MANAGE_ALL', 'Review manage all', 'System permission REVIEW_MANAGE_ALL', 'REVIEW', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('QUESTION_CREATE_SELF', 'Question create self', 'System permission QUESTION_CREATE_SELF', 'QUESTION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('QUESTION_UPDATE_SELF', 'Question update self', 'System permission QUESTION_UPDATE_SELF', 'QUESTION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('QUESTION_DELETE_SELF', 'Question delete self', 'System permission QUESTION_DELETE_SELF', 'QUESTION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('QUESTION_MODERATE', 'Question moderate', 'System permission QUESTION_MODERATE', 'QUESTION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('QUESTION_MANAGE_ALL', 'Question manage all', 'System permission QUESTION_MANAGE_ALL', 'QUESTION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('NOTIFICATION_READ_SELF', 'Notification read self', 'System permission NOTIFICATION_READ_SELF', 'NOTIFICATION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('NOTIFICATION_UPDATE_SELF', 'Notification update self', 'System permission NOTIFICATION_UPDATE_SELF', 'NOTIFICATION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('NOTIFICATION_SEND_ALL', 'Notification send all', 'System permission NOTIFICATION_SEND_ALL', 'NOTIFICATION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('NOTIFICATION_MANAGE_ALL', 'Notification manage all', 'System permission NOTIFICATION_MANAGE_ALL', 'NOTIFICATION', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('SUPPORT_TICKET_CREATE_SELF', 'Support ticket create self', 'System permission SUPPORT_TICKET_CREATE_SELF', 'SUPPORT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('SUPPORT_TICKET_READ_SELF', 'Support ticket read self', 'System permission SUPPORT_TICKET_READ_SELF', 'SUPPORT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('SUPPORT_TICKET_READ_ALL', 'Support ticket read all', 'System permission SUPPORT_TICKET_READ_ALL', 'SUPPORT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('SUPPORT_TICKET_MANAGE_ALL', 'Support ticket manage all', 'System permission SUPPORT_TICKET_MANAGE_ALL', 'SUPPORT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CMS_READ', 'Cms read', 'System permission CMS_READ', 'CMS', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CMS_CREATE', 'Cms create', 'System permission CMS_CREATE', 'CMS', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CMS_UPDATE', 'Cms update', 'System permission CMS_UPDATE', 'CMS', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CMS_DELETE', 'Cms delete', 'System permission CMS_DELETE', 'CMS', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CMS_MANAGE_ALL', 'Cms manage all', 'System permission CMS_MANAGE_ALL', 'CMS', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('DASHBOARD_READ_ADMIN', 'Dashboard read admin', 'System permission DASHBOARD_READ_ADMIN', 'REPORT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REPORT_READ_ALL', 'Report read all', 'System permission REPORT_READ_ALL', 'REPORT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REPORT_EXPORT_ALL', 'Report export all', 'System permission REPORT_EXPORT_ALL', 'REPORT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('AUDIT_LOG_READ', 'Audit log read', 'System permission AUDIT_LOG_READ', 'AUDIT', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('SYSTEM_CONFIG_READ', 'System config read', 'System permission SYSTEM_CONFIG_READ', 'SYSTEM', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('SYSTEM_CONFIG_UPDATE', 'System config update', 'System permission SYSTEM_CONFIG_UPDATE', 'SYSTEM', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('SYSTEM_CONFIG_MANAGE', 'System config manage', 'System permission SYSTEM_CONFIG_MANAGE', 'SYSTEM', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ROLE_MANAGE', 'Role manage', 'System permission ROLE_MANAGE', 'SYSTEM', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PERMISSION_MANAGE', 'Permission manage', 'System permission PERMISSION_MANAGE', 'SYSTEM', b'1', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
WHERE r.code = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'USER_READ_SELF', 'USER_UPDATE_SELF', 'USER_CHANGE_PASSWORD_SELF', 'USER_DEACTIVATE_SELF',
    'ADDRESS_READ_SELF', 'ADDRESS_CREATE_SELF', 'ADDRESS_UPDATE_SELF', 'ADDRESS_DELETE_SELF',
    'ADDRESS_SET_DEFAULT_SELF', 'WISHLIST_READ_SELF', 'WISHLIST_UPDATE_SELF', 'CART_READ_SELF',
    'CART_UPDATE_SELF', 'CART_CLEAR_SELF', 'CHECKOUT_CREATE_SELF', 'CHECKOUT_APPLY_COUPON_SELF',
    'ORDER_READ_SELF', 'ORDER_CANCEL_SELF', 'PAYMENT_CREATE_SELF', 'PAYMENT_READ_SELF',
    'PAYMENT_RETRY_SELF', 'REFUND_CREATE_SELF', 'REFUND_READ_SELF', 'REVIEW_CREATE_SELF',
    'REVIEW_UPDATE_SELF', 'REVIEW_DELETE_SELF', 'QUESTION_CREATE_SELF', 'SUPPORT_TICKET_CREATE_SELF',
    'SUPPORT_TICKET_READ_SELF'
)
WHERE r.code = 'USER';
