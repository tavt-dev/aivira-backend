package com.tien.aivirabackend.constant;

public enum PermissionCode {
    USER_READ_SELF(PermissionGroup.USER),
    USER_UPDATE_SELF(PermissionGroup.USER),
    USER_CHANGE_PASSWORD_SELF(PermissionGroup.USER),
    USER_DEACTIVATE_SELF(PermissionGroup.USER),
    USER_READ_ALL(PermissionGroup.USER),
    USER_LOCK(PermissionGroup.USER),
    USER_UNLOCK(PermissionGroup.USER),
    USER_ASSIGN_ROLE(PermissionGroup.USER),
    USER_PERMISSION_READ(PermissionGroup.USER),
    USER_PERMISSION_GRANT(PermissionGroup.USER),
    USER_PERMISSION_REVOKE(PermissionGroup.USER),
    USER_PERMISSION_MANAGE(PermissionGroup.USER),
    USER_MANAGE_ALL(PermissionGroup.USER),

    ADDRESS_READ_SELF(PermissionGroup.ADDRESS),
    ADDRESS_CREATE_SELF(PermissionGroup.ADDRESS),
    ADDRESS_UPDATE_SELF(PermissionGroup.ADDRESS),
    ADDRESS_DELETE_SELF(PermissionGroup.ADDRESS),
    ADDRESS_SET_DEFAULT_SELF(PermissionGroup.ADDRESS),

    SELLER_APPLY(PermissionGroup.SHOP),
    SHOP_READ_SELF(PermissionGroup.SHOP),
    SHOP_UPDATE_SELF(PermissionGroup.SHOP),
    SHOP_READ_ALL(PermissionGroup.SHOP),
    SHOP_APPROVE(PermissionGroup.SHOP),
    SHOP_REJECT(PermissionGroup.SHOP),
    SHOP_LOCK(PermissionGroup.SHOP),
    SHOP_UNLOCK(PermissionGroup.SHOP),
    SHOP_MANAGE_ALL(PermissionGroup.SHOP),

    CATEGORY_READ(PermissionGroup.CATEGORY),
    CATEGORY_CREATE(PermissionGroup.CATEGORY),
    CATEGORY_UPDATE(PermissionGroup.CATEGORY),
    CATEGORY_DELETE(PermissionGroup.CATEGORY),
    CATEGORY_REORDER(PermissionGroup.CATEGORY),
    CATEGORY_MANAGE_ALL(PermissionGroup.CATEGORY),

    PRODUCT_READ(PermissionGroup.PRODUCT),
    PRODUCT_CREATE_OWN_SHOP(PermissionGroup.PRODUCT),
    PRODUCT_UPDATE_OWN_SHOP(PermissionGroup.PRODUCT),
    PRODUCT_DELETE_OWN_SHOP(PermissionGroup.PRODUCT),
    PRODUCT_SUBMIT_REVIEW_OWN_SHOP(PermissionGroup.PRODUCT),
    PRODUCT_APPROVE(PermissionGroup.PRODUCT),
    PRODUCT_REJECT(PermissionGroup.PRODUCT),
    PRODUCT_MANAGE_ALL(PermissionGroup.PRODUCT),

    PRODUCT_MEDIA_UPLOAD_OWN_SHOP(PermissionGroup.PRODUCT),
    PRODUCT_MEDIA_UPDATE_OWN_SHOP(PermissionGroup.PRODUCT),
    PRODUCT_MEDIA_DELETE_OWN_SHOP(PermissionGroup.PRODUCT),
    PRODUCT_MEDIA_MANAGE_ALL(PermissionGroup.PRODUCT),

    INVENTORY_READ_OWN_SHOP(PermissionGroup.INVENTORY),
    INVENTORY_UPDATE_OWN_SHOP(PermissionGroup.INVENTORY),
    INVENTORY_ADJUST_OWN_SHOP(PermissionGroup.INVENTORY),
    INVENTORY_READ_ALL(PermissionGroup.INVENTORY),
    INVENTORY_MANAGE_ALL(PermissionGroup.INVENTORY),

    WISHLIST_READ_SELF(PermissionGroup.USER),
    WISHLIST_UPDATE_SELF(PermissionGroup.USER),

    CART_READ_SELF(PermissionGroup.CART),
    CART_UPDATE_SELF(PermissionGroup.CART),
    CART_CLEAR_SELF(PermissionGroup.CART),

    CHECKOUT_CREATE_SELF(PermissionGroup.CHECKOUT),
    CHECKOUT_APPLY_COUPON_SELF(PermissionGroup.CHECKOUT),

    ORDER_READ_SELF(PermissionGroup.ORDER),
    ORDER_CANCEL_SELF(PermissionGroup.ORDER),
    ORDER_READ_OWN_SHOP(PermissionGroup.ORDER),
    ORDER_CONFIRM_OWN_SHOP(PermissionGroup.ORDER),
    ORDER_UPDATE_STATUS_OWN_SHOP(PermissionGroup.ORDER),
    ORDER_READ_ALL(PermissionGroup.ORDER),
    ORDER_UPDATE_STATUS_ALL(PermissionGroup.ORDER),
    ORDER_CANCEL_ALL(PermissionGroup.ORDER),
    ORDER_MANAGE_ALL(PermissionGroup.ORDER),

    PAYMENT_CREATE_SELF(PermissionGroup.PAYMENT),
    PAYMENT_READ_SELF(PermissionGroup.PAYMENT),
    PAYMENT_READ_ALL(PermissionGroup.PAYMENT),
    PAYMENT_CALLBACK_PROCESS(PermissionGroup.PAYMENT),
    PAYMENT_RETRY_SELF(PermissionGroup.PAYMENT),
    PAYMENT_RECONCILE(PermissionGroup.PAYMENT),
    PAYMENT_MANAGE_ALL(PermissionGroup.PAYMENT),

    REFUND_CREATE_SELF(PermissionGroup.REFUND),
    REFUND_READ_SELF(PermissionGroup.REFUND),
    REFUND_READ_OWN_SHOP(PermissionGroup.REFUND),
    REFUND_APPROVE_OWN_SHOP(PermissionGroup.REFUND),
    REFUND_REJECT_OWN_SHOP(PermissionGroup.REFUND),
    REFUND_READ_ALL(PermissionGroup.REFUND),
    REFUND_MANAGE_ALL(PermissionGroup.REFUND),

    SHIPPING_READ_SELF(PermissionGroup.SHIPPING),
    SHIPPING_READ_OWN_SHOP(PermissionGroup.SHIPPING),
    SHIPPING_UPDATE_OWN_SHOP(PermissionGroup.SHIPPING),
    SHIPPING_READ_ALL(PermissionGroup.SHIPPING),
    SHIPPING_MANAGE_ALL(PermissionGroup.SHIPPING),

    COUPON_APPLY_SELF(PermissionGroup.COUPON),
    COUPON_CREATE_OWN_SHOP(PermissionGroup.COUPON),
    COUPON_UPDATE_OWN_SHOP(PermissionGroup.COUPON),
    COUPON_DELETE_OWN_SHOP(PermissionGroup.COUPON),
    COUPON_CREATE_ALL(PermissionGroup.COUPON),
    COUPON_UPDATE_ALL(PermissionGroup.COUPON),
    COUPON_DELETE_ALL(PermissionGroup.COUPON),
    COUPON_MANAGE_ALL(PermissionGroup.COUPON),

    PROMOTION_READ(PermissionGroup.PROMOTION),
    PROMOTION_CREATE_OWN_SHOP(PermissionGroup.PROMOTION),
    PROMOTION_UPDATE_OWN_SHOP(PermissionGroup.PROMOTION),
    PROMOTION_DELETE_OWN_SHOP(PermissionGroup.PROMOTION),
    PROMOTION_CREATE_ALL(PermissionGroup.PROMOTION),
    PROMOTION_UPDATE_ALL(PermissionGroup.PROMOTION),
    PROMOTION_DELETE_ALL(PermissionGroup.PROMOTION),
    PROMOTION_MANAGE_ALL(PermissionGroup.PROMOTION),

    REVIEW_CREATE_SELF(PermissionGroup.REVIEW),
    REVIEW_UPDATE_SELF(PermissionGroup.REVIEW),
    REVIEW_DELETE_SELF(PermissionGroup.REVIEW),
    REVIEW_REPLY_OWN_SHOP(PermissionGroup.REVIEW),
    REVIEW_READ_ALL(PermissionGroup.REVIEW),
    REVIEW_MODERATE(PermissionGroup.REVIEW),
    REVIEW_MANAGE_ALL(PermissionGroup.REVIEW),

    QUESTION_CREATE_SELF(PermissionGroup.QUESTION),
    QUESTION_UPDATE_SELF(PermissionGroup.QUESTION),
    QUESTION_DELETE_SELF(PermissionGroup.QUESTION),
    QUESTION_REPLY_OWN_SHOP(PermissionGroup.QUESTION),
    QUESTION_MODERATE(PermissionGroup.QUESTION),
    QUESTION_MANAGE_ALL(PermissionGroup.QUESTION),

    NOTIFICATION_READ_SELF(PermissionGroup.NOTIFICATION),
    NOTIFICATION_UPDATE_SELF(PermissionGroup.NOTIFICATION),
    NOTIFICATION_SEND_ALL(PermissionGroup.NOTIFICATION),
    NOTIFICATION_MANAGE_ALL(PermissionGroup.NOTIFICATION),

    SUPPORT_TICKET_CREATE_SELF(PermissionGroup.SUPPORT),
    SUPPORT_TICKET_READ_SELF(PermissionGroup.SUPPORT),
    SUPPORT_TICKET_READ_OWN_SHOP(PermissionGroup.SUPPORT),
    SUPPORT_TICKET_REPLY_OWN_SHOP(PermissionGroup.SUPPORT),
    SUPPORT_TICKET_READ_ALL(PermissionGroup.SUPPORT),
    SUPPORT_TICKET_MANAGE_ALL(PermissionGroup.SUPPORT),

    CMS_READ(PermissionGroup.CMS),
    CMS_CREATE(PermissionGroup.CMS),
    CMS_UPDATE(PermissionGroup.CMS),
    CMS_DELETE(PermissionGroup.CMS),
    CMS_MANAGE_ALL(PermissionGroup.CMS),

    DASHBOARD_READ_SELLER(PermissionGroup.REPORT),
    DASHBOARD_READ_ADMIN(PermissionGroup.REPORT),
    REPORT_READ_OWN_SHOP(PermissionGroup.REPORT),
    REPORT_READ_ALL(PermissionGroup.REPORT),
    REPORT_EXPORT_ALL(PermissionGroup.REPORT),

    AUDIT_LOG_READ(PermissionGroup.AUDIT),
    SYSTEM_CONFIG_READ(PermissionGroup.SYSTEM),
    SYSTEM_CONFIG_UPDATE(PermissionGroup.SYSTEM),
    SYSTEM_CONFIG_MANAGE(PermissionGroup.SYSTEM),
    ROLE_MANAGE(PermissionGroup.SYSTEM),
    PERMISSION_MANAGE(PermissionGroup.SYSTEM);

    private final PermissionGroup group;

    PermissionCode(PermissionGroup group) {
        this.group = group;
    }

    public PermissionGroup getGroup() {
        return group;
    }
}
