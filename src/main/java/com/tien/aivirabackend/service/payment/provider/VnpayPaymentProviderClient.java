package com.tien.aivirabackend.service.payment.provider;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.tien.aivirabackend.config.properties.VnpayPaymentProperties;
import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CheckoutErrorCode;
import com.tien.aivirabackend.service.payment.PaymentSignatureUtils;

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
    RestClient paymentRestClient;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.VNPAY;
    }

    @Override
    public PaymentProviderResult createPayment(PaymentProviderRequest request) {
        requireEnabled();
        PaymentAttempt attempt = request.paymentAttempt();
        String txnRef = attempt.getProviderTxnRef();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put(
                "vnp_Amount",
                attempt.getAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .toBigInteger()
                        .toString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan " + request.paymentGroup().getPaymentCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.getReturnUrl());
        params.put("vnp_IpAddr", request.requestMetadata().clientIp());
        Instant createdAt = attempt.getCreatedAt() == null ? Instant.now() : attempt.getCreatedAt();
        params.put("vnp_CreateDate", VNPAY_TIME.format(createdAt));
        if (attempt.getExpiresAt() != null) {
            params.put("vnp_ExpireDate", VNPAY_TIME.format(attempt.getExpiresAt()));
        }
        String hashData = buildQuery(params, true);
        String secureHash = PaymentSignatureUtils.hmacSha512(hashData, properties.getHashSecret());
        String paymentUrl =
                properties.getPaymentUrl() + "?" + buildQuery(params, true) + "&vnp_SecureHash=" + secureHash;
        return new PaymentProviderResult(txnRef, null, null, paymentUrl, null, null, params.toString(), null);
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

    @Override
    public PaymentProviderCallbackResult parseCallback(Map<String, ?> payload) {
        String txnRef = stringValue(payload.get("vnp_TxnRef"));
        BigDecimal amount =
                new BigDecimal(defaultString(payload.get("vnp_Amount"), "0")).divide(BigDecimal.valueOf(100));
        boolean success = "00".equals(stringValue(payload.get("vnp_ResponseCode")))
                && "00".equals(stringValue(payload.get("vnp_TransactionStatus")));
        String transactionId = stringValue(payload.get("vnp_TransactionNo"));
        String eventKey = "VNPAY:" + txnRef + ":" + (transactionId.isBlank() ? payload.toString() : transactionId);
        return new PaymentProviderCallbackResult(
                txnRef,
                null,
                transactionId,
                amount,
                success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
                eventKey,
                payload);
    }

    @Override
    public PaymentProviderQueryResult queryPayment(PaymentAttempt attempt) {
        requireEnabled();
        if (!StringUtils.hasText(properties.getTransactionUrl())) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_PROVIDER_DISABLED);
        }
        Instant now = Instant.now();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_RequestId", attempt.getProviderTxnRef() + "-QUERY-" + now.toEpochMilli());
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "querydr");
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_TxnRef", attempt.getProviderTxnRef());
        params.put("vnp_OrderInfo", "Query payment " + attempt.getProviderTxnRef());
        params.put(
                "vnp_TransactionDate",
                VNPAY_TIME.format(attempt.getCreatedAt() == null ? now : attempt.getCreatedAt()));
        params.put("vnp_CreateDate", VNPAY_TIME.format(now));
        params.put("vnp_IpAddr", "127.0.0.1");
        String secureHash = PaymentSignatureUtils.hmacSha512(buildQuery(params, false), properties.getHashSecret());
        params.put("vnp_SecureHash", secureHash);
        try {
            Map<String, Object> response = paymentRestClient
                    .post()
                    .uri(properties.getTransactionUrl())
                    .body(params)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null) {
                throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_PROVIDER_ERROR);
            }
            String transactionStatus = stringValue(response.get("vnp_TransactionStatus"));
            PaymentStatus status = "00".equals(transactionStatus) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            BigDecimal amount =
                    new BigDecimal(defaultString(response.get("vnp_Amount"), "0")).divide(BigDecimal.valueOf(100));
            return new PaymentProviderQueryResult(
                    attempt.getProviderTxnRef(),
                    attempt.getRequestId(),
                    stringValue(response.get("vnp_TransactionNo")),
                    amount,
                    status,
                    stringValue(response.get("vnp_Message")),
                    response.toString());
        } catch (RestClientException ex) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_PROVIDER_ERROR);
        }
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

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultString(Object value, String fallback) {
        String candidate = stringValue(value);
        return candidate.isBlank() ? fallback : candidate;
    }
}
