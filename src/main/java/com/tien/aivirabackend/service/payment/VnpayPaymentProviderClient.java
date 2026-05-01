package com.tien.aivirabackend.service.payment;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.config.properties.VnpayPaymentProperties;
import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CheckoutErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VnpayPaymentProviderClient implements PaymentProviderClient {
    private static final DateTimeFormatter VNPAY_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    VnpayPaymentProperties properties;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.VNPAY;
    }

    @Override
    public PaymentProviderResult createPayment(PaymentGroup paymentGroup) {
        requireEnabled();
        String txnRef = paymentGroup.getPaymentCode() + "-" + System.currentTimeMillis();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put(
                "vnp_Amount",
                paymentGroup
                        .getAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .toBigInteger()
                        .toString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan " + paymentGroup.getPaymentCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.getReturnUrl());
        params.put("vnp_IpAddr", "127.0.0.1");
        Instant createdAt = paymentGroup.getCreatedAt() == null ? Instant.now() : paymentGroup.getCreatedAt();
        params.put("vnp_CreateDate", VNPAY_TIME.format(createdAt));
        if (paymentGroup.getExpiresAt() != null) {
            params.put("vnp_ExpireDate", VNPAY_TIME.format(paymentGroup.getExpiresAt()));
        }
        String hashData = buildQuery(params, true);
        String secureHash = PaymentSignatureUtils.hmacSha512(hashData, properties.getHashSecret());
        String paymentUrl =
                properties.getPaymentUrl() + "?" + buildQuery(params, true) + "&vnp_SecureHash=" + secureHash;
        return new PaymentProviderResult(txnRef, null, paymentUrl, null, null, null);
    }

    @Override
    public boolean verifyCallback(Map<String, ?> payload) {
        requireEnabled();
        Object secureHashValue = payload.get("vnp_SecureHash");
        if (secureHashValue == null) {
            return false;
        }
        Map<String, String> params = new TreeMap<>();
        payload.forEach((key, value) -> {
            if (key.startsWith("vnp_") && !"vnp_SecureHash".equals(key) && !"vnp_SecureHashType".equals(key)) {
                params.put(key, String.valueOf(value));
            }
        });
        String expected = PaymentSignatureUtils.hmacSha512(buildQuery(params, true), properties.getHashSecret());
        return expected.equalsIgnoreCase(String.valueOf(secureHashValue));
    }

    private String buildQuery(Map<String, String> params, boolean encode) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + (encode ? urlEncode(entry.getValue()) : entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()
                || !StringUtils.hasText(properties.getPaymentUrl())
                || !StringUtils.hasText(properties.getTmnCode())
                || !StringUtils.hasText(properties.getHashSecret())
                || !StringUtils.hasText(properties.getReturnUrl())) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_PROVIDER_DISABLED);
        }
    }
}
