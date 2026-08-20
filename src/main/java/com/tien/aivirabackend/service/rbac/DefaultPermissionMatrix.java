package com.tien.aivirabackend.service.rbac;

import java.util.EnumSet;
import java.util.Set;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.constant.PredefinedRole;

public final class DefaultPermissionMatrix {
    private DefaultPermissionMatrix() {
    }

    public static Set<PermissionCode> forRole(PredefinedRole role) {
        return switch (role) {
        case USER -> userPermissions();
        case ADMIN -> EnumSet.allOf(PermissionCode.class);
        };
    }

    private static EnumSet<PermissionCode> userPermissions() {
        return EnumSet.of(PermissionCode.USER_READ_SELF, PermissionCode.USER_UPDATE_SELF,
                PermissionCode.USER_CHANGE_PASSWORD_SELF, PermissionCode.USER_DEACTIVATE_SELF,
                PermissionCode.ADDRESS_READ_SELF, PermissionCode.ADDRESS_CREATE_SELF,
                PermissionCode.ADDRESS_UPDATE_SELF, PermissionCode.ADDRESS_DELETE_SELF,
                PermissionCode.ADDRESS_SET_DEFAULT_SELF, PermissionCode.WISHLIST_READ_SELF,
                PermissionCode.WISHLIST_UPDATE_SELF, PermissionCode.CART_READ_SELF, PermissionCode.CART_UPDATE_SELF,
                PermissionCode.CART_CLEAR_SELF, PermissionCode.CHECKOUT_CREATE_SELF,
                PermissionCode.CHECKOUT_APPLY_COUPON_SELF, PermissionCode.ORDER_READ_SELF,
                PermissionCode.ORDER_CANCEL_SELF, PermissionCode.PAYMENT_CREATE_SELF, PermissionCode.PAYMENT_READ_SELF,
                PermissionCode.PAYMENT_RETRY_SELF, PermissionCode.REFUND_CREATE_SELF, PermissionCode.REFUND_READ_SELF,
                PermissionCode.REVIEW_CREATE_SELF, PermissionCode.REVIEW_UPDATE_SELF, PermissionCode.REVIEW_DELETE_SELF,
                PermissionCode.QUESTION_CREATE_SELF, PermissionCode.SUPPORT_TICKET_CREATE_SELF,
                PermissionCode.SUPPORT_TICKET_READ_SELF);
    }
}
