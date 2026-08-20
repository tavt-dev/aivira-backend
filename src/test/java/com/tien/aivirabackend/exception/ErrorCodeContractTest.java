package com.tien.aivirabackend.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tien.aivirabackend.exception.errorCode.AccountErrorCode;
import com.tien.aivirabackend.exception.errorCode.AddressErrorCode;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.CartErrorCode;
import com.tien.aivirabackend.exception.errorCode.CategoryErrorCode;
import com.tien.aivirabackend.exception.errorCode.CheckoutErrorCode;
import com.tien.aivirabackend.exception.errorCode.CommonErrorCode;
import com.tien.aivirabackend.exception.errorCode.CouponErrorCode;
import com.tien.aivirabackend.exception.errorCode.EmailErrorCode;
import com.tien.aivirabackend.exception.errorCode.FileValidationErrorCode;
import com.tien.aivirabackend.exception.errorCode.JwtErrorCode;
import com.tien.aivirabackend.exception.errorCode.OrderErrorCode;
import com.tien.aivirabackend.exception.errorCode.OtpErrorCode;
import com.tien.aivirabackend.exception.errorCode.PasswordErrorCode;
import com.tien.aivirabackend.exception.errorCode.PaymentErrorCode;
import com.tien.aivirabackend.exception.errorCode.ProductErrorCode;
import com.tien.aivirabackend.exception.errorCode.PromotionErrorCode;
import com.tien.aivirabackend.exception.errorCode.ReviewErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;

class ErrorCodeContractTest {
    private static final List<Class<? extends Enum<?>>> ERROR_ENUMS = List.of(AccountErrorCode.class,
            AddressErrorCode.class, AuthErrorCode.class, CartErrorCode.class, CategoryErrorCode.class,
            CheckoutErrorCode.class, CommonErrorCode.class, CouponErrorCode.class, EmailErrorCode.class,
            FileValidationErrorCode.class, JwtErrorCode.class, OrderErrorCode.class, OtpErrorCode.class,
            PasswordErrorCode.class, PaymentErrorCode.class, ProductErrorCode.class, PromotionErrorCode.class,
            ReviewErrorCode.class, UserErrorCode.class);

    @Test
    void allErrorCodesHaveCompleteAndUniqueContractValues() {
        Map<String, String> seenCodes = new HashMap<>();

        for (Class<? extends Enum<?>> enumClass : ERROR_ENUMS) {
            for (Enum<?> constant : enumClass.getEnumConstants()) {
                ErrorCode errorCode = (ErrorCode) constant;

                assertThat(errorCode.getCode()).as(enumClass.getSimpleName() + "." + constant.name()).isNotBlank();
                assertThat(errorCode.getMessage()).as(enumClass.getSimpleName() + "." + constant.name()).isNotBlank();
                assertThat(errorCode.getHttpStatus()).as(enumClass.getSimpleName() + "." + constant.name()).isNotNull();

                String previous = seenCodes.putIfAbsent(errorCode.getCode(),
                        enumClass.getSimpleName() + "." + constant.name());
                assertThat(previous).as("Duplicate error code %s used by %s and %s", errorCode.getCode(), previous,
                        enumClass.getSimpleName() + "." + constant.name()).isNull();
            }
        }
    }

    @Test
    void phaseNineImportantDomainErrorsExist() {
        assertThat(ProductErrorCode.PRODUCT_ISBN_ALREADY_EXISTS).isNotNull();
        assertThat(ProductErrorCode.PRODUCT_INVALID_PUBLICATION_YEAR).isNotNull();
        assertThat(ProductErrorCode.PRODUCT_INVALID_PAGE_COUNT).isNotNull();
        assertThat(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION).isNotNull();
        assertThat(OrderErrorCode.ORDER_CANCEL_REQUIRES_REFUND).isNotNull();
        assertThat(OrderErrorCode.ORDER_REFUND_NOT_ALLOWED).isNotNull();
        assertThat(OrderErrorCode.ORDER_REFUND_ALREADY_PROCESSED).isNotNull();
        assertThat(OrderErrorCode.ORDER_REFUND_AMOUNT_INVALID).isNotNull();
        assertThat(CouponErrorCode.COUPON_INVALID).isNotNull();
        assertThat(CouponErrorCode.COUPON_EXPIRED).isNotNull();
        assertThat(CouponErrorCode.COUPON_USAGE_LIMIT_EXCEEDED).isNotNull();
        assertThat(ReviewErrorCode.REVIEW_NOT_ALLOWED).isNotNull();
    }
}
