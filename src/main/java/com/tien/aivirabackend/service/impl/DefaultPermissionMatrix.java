package com.tien.aivirabackend.service.impl;

import java.util.EnumSet;
import java.util.Set;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.constant.PredefinedRole;

final class DefaultPermissionMatrix {
    private DefaultPermissionMatrix() {}

    static Set<PermissionCode> forRole(PredefinedRole role) {
        return switch (role) {
            case USER -> userPermissions();
            case SELLER -> sellerPermissions();
            case ADMIN -> EnumSet.allOf(PermissionCode.class);
        };
    }

    private static EnumSet<PermissionCode> userPermissions() {
        return EnumSet.of(
                PermissionCode.USER_READ_SELF,
                PermissionCode.USER_UPDATE_SELF,
                PermissionCode.USER_CHANGE_PASSWORD_SELF,
                PermissionCode.USER_DEACTIVATE_SELF,
                PermissionCode.ADDRESS_READ_SELF,
                PermissionCode.ADDRESS_CREATE_SELF,
                PermissionCode.ADDRESS_UPDATE_SELF,
                PermissionCode.ADDRESS_DELETE_SELF,
                PermissionCode.ADDRESS_SET_DEFAULT_SELF,
                PermissionCode.WISHLIST_READ_SELF,
                PermissionCode.WISHLIST_UPDATE_SELF,
                PermissionCode.CART_READ_SELF,
                PermissionCode.CART_UPDATE_SELF,
                PermissionCode.CART_CLEAR_SELF,
                PermissionCode.CHECKOUT_CREATE_SELF,
                PermissionCode.CHECKOUT_APPLY_COUPON_SELF,
                PermissionCode.ORDER_READ_SELF,
                PermissionCode.ORDER_CANCEL_SELF,
                PermissionCode.PAYMENT_CREATE_SELF,
                PermissionCode.PAYMENT_READ_SELF,
                PermissionCode.PAYMENT_RETRY_SELF,
                PermissionCode.REFUND_CREATE_SELF,
                PermissionCode.REFUND_READ_SELF,
                PermissionCode.REVIEW_CREATE_SELF,
                PermissionCode.REVIEW_UPDATE_SELF,
                PermissionCode.REVIEW_DELETE_SELF,
                PermissionCode.QUESTION_CREATE_SELF,
                PermissionCode.SUPPORT_TICKET_CREATE_SELF,
                PermissionCode.SUPPORT_TICKET_READ_SELF);
    }

    private static EnumSet<PermissionCode> sellerPermissions() {
        EnumSet<PermissionCode> permissions = userPermissions();
        permissions.addAll(EnumSet.of(
                PermissionCode.SELLER_APPLY,
                PermissionCode.SHOP_READ_SELF,
                PermissionCode.SHOP_UPDATE_SELF,
                PermissionCode.PRODUCT_CREATE_OWN_SHOP,
                PermissionCode.PRODUCT_UPDATE_OWN_SHOP,
                PermissionCode.PRODUCT_DELETE_OWN_SHOP,
                PermissionCode.PRODUCT_SUBMIT_REVIEW_OWN_SHOP,
                PermissionCode.PRODUCT_MEDIA_UPLOAD_OWN_SHOP,
                PermissionCode.PRODUCT_MEDIA_UPDATE_OWN_SHOP,
                PermissionCode.PRODUCT_MEDIA_DELETE_OWN_SHOP,
                PermissionCode.INVENTORY_READ_OWN_SHOP,
                PermissionCode.INVENTORY_UPDATE_OWN_SHOP,
                PermissionCode.INVENTORY_ADJUST_OWN_SHOP,
                PermissionCode.ORDER_READ_OWN_SHOP,
                PermissionCode.ORDER_CONFIRM_OWN_SHOP,
                PermissionCode.ORDER_UPDATE_STATUS_OWN_SHOP,
                PermissionCode.REFUND_READ_OWN_SHOP,
                PermissionCode.REFUND_APPROVE_OWN_SHOP,
                PermissionCode.REFUND_REJECT_OWN_SHOP,
                PermissionCode.SHIPPING_READ_OWN_SHOP,
                PermissionCode.SHIPPING_UPDATE_OWN_SHOP,
                PermissionCode.COUPON_CREATE_OWN_SHOP,
                PermissionCode.COUPON_UPDATE_OWN_SHOP,
                PermissionCode.COUPON_DELETE_OWN_SHOP,
                PermissionCode.PROMOTION_CREATE_OWN_SHOP,
                PermissionCode.PROMOTION_UPDATE_OWN_SHOP,
                PermissionCode.PROMOTION_DELETE_OWN_SHOP,
                PermissionCode.REVIEW_REPLY_OWN_SHOP,
                PermissionCode.QUESTION_REPLY_OWN_SHOP,
                PermissionCode.DASHBOARD_READ_SELLER,
                PermissionCode.REPORT_READ_OWN_SHOP));
        return permissions;
    }
}
