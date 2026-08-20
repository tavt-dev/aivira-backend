package com.tien.aivirabackend.service.payment.provider;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.tien.aivirabackend.config.properties.MomoPaymentProperties;
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
public class MomoPaymentProviderClient implements PaymentProviderClient {
    MomoPaymentProperties properties;
    RestClient paymentRestClient;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.MOMO;
    }

    @Override
    public PaymentProviderResult createPayment(PaymentProviderRequest providerRequest) {
        requireEnabled();
        PaymentAttempt attempt = providerRequest.paymentAttempt();
        String requestId = attempt.getRequestId();
        String orderId = attempt.getProviderTxnRef();
        String amount = attempt.getAmount().toBigInteger().toString();
        String extraData = "";
        String orderInfo = "Thanh toan " + providerRequest.paymentGroup().getPaymentCode();
        String rawSignature = "accessKey=" + properties.getAccessKey() + "&amount=" + amount + "&extraData=" + extraData
                + "&ipnUrl=" + properties.getIpnUrl() + "&orderId=" + orderId + "&orderInfo=" + orderInfo
                + "&partnerCode=" + properties.getPartnerCode() + "&redirectUrl=" + properties.getRedirectUrl()
                + "&requestId=" + requestId + "&requestType=" + properties.getRequestType();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("partnerCode", properties.getPartnerCode());
        request.put("partnerName", "Aivira");
        request.put("storeId", "Aivira");
        request.put("requestId", requestId);
        request.put("amount", Long.parseLong(amount));
        request.put("orderId", orderId);
        request.put("orderInfo", orderInfo);
        request.put("redirectUrl", properties.getRedirectUrl());
        request.put("ipnUrl", properties.getIpnUrl());
        request.put("requestType", properties.getRequestType());
        request.put("extraData", extraData);
        request.put("lang", "vi");
        request.put("signature", PaymentSignatureUtils.hmacSha256(rawSignature, properties.getSecretKey()));

        try {
            Map<String, Object> response = paymentRestClient.post().uri(properties.getEndpoint()).body(request)
                    .retrieve().body(new ParameterizedTypeReference<>() {
                    });
            if (response == null) {
                throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_PROVIDER_ERROR);
            }
            return new PaymentProviderResult(orderId, requestId, stringValue(response.get("transId")),
                    stringValue(response.get("payUrl")), stringValue(response.get("deeplink")),
                    stringValue(response.get("qrCodeUrl")), request.toString(), response.toString());
        } catch (RestClientException ex) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_PROVIDER_ERROR);
        }
    }

    @Override
    public boolean verifyCallback(Map<String, ?> payload) {
        requireEnabled();
        Object signature = payload.get("signature");
        if (signature == null) {
            return false;
        }
        String rawSignature = "accessKey=" + properties.getAccessKey() + "&amount=" + stringValue(payload.get("amount"))
                + "&extraData=" + stringValue(payload.get("extraData")) + "&message="
                + stringValue(payload.get("message")) + "&orderId=" + stringValue(payload.get("orderId"))
                + "&orderInfo=" + stringValue(payload.get("orderInfo")) + "&orderType="
                + stringValue(payload.get("orderType")) + "&partnerClientId="
                + stringValue(payload.get("partnerClientId")) + "&partnerCode="
                + stringValue(payload.get("partnerCode")) + "&payType=" + stringValue(payload.get("payType"))
                + "&requestId=" + stringValue(payload.get("requestId")) + "&responseTime="
                + stringValue(payload.get("responseTime")) + "&resultCode=" + stringValue(payload.get("resultCode"))
                + "&transId=" + stringValue(payload.get("transId"));
        return PaymentSignatureUtils.hmacSha256(rawSignature, properties.getSecretKey())
                .equalsIgnoreCase(String.valueOf(signature));
    }

    @Override
    public PaymentProviderCallbackResult parseCallback(Map<String, ?> payload) {
        String orderId = stringValue(payload.get("orderId"));
        String requestId = stringValue(payload.get("requestId"));
        String transId = stringValue(payload.get("transId"));
        String eventKey = "MOMO:" + orderId + ":" + (transId.isBlank() ? requestId : transId);
        return new PaymentProviderCallbackResult(orderId, requestId, transId,
                new java.math.BigDecimal(stringValue(payload.get("amount"))),
                "0".equals(stringValue(payload.get("resultCode"))) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
                eventKey, payload);
    }

    @Override
    public PaymentProviderQueryResult queryPayment(PaymentAttempt attempt) {
        requireEnabled();
        if (!StringUtils.hasText(properties.getQueryEndpoint())) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_PROVIDER_DISABLED);
        }
        String rawSignature = "accessKey=" + properties.getAccessKey() + "&orderId=" + attempt.getProviderTxnRef()
                + "&partnerCode=" + properties.getPartnerCode() + "&requestId=" + attempt.getRequestId();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("partnerCode", properties.getPartnerCode());
        request.put("requestId", attempt.getRequestId());
        request.put("orderId", attempt.getProviderTxnRef());
        request.put("lang", "vi");
        request.put("signature", PaymentSignatureUtils.hmacSha256(rawSignature, properties.getSecretKey()));
        try {
            Map<String, Object> response = paymentRestClient.post().uri(properties.getQueryEndpoint()).body(request)
                    .retrieve().body(new ParameterizedTypeReference<>() {
                    });
            if (response == null) {
                throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_PROVIDER_ERROR);
            }
            return new PaymentProviderQueryResult(attempt.getProviderTxnRef(), attempt.getRequestId(),
                    stringValue(response.get("transId")), new java.math.BigDecimal(stringValue(response.get("amount"))),
                    "0".equals(stringValue(response.get("resultCode"))) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
                    stringValue(response.get("message")), response.toString());
        } catch (RestClientException ex) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_PROVIDER_ERROR);
        }
    }

    private void requireEnabled() {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getPartnerCode()) || !StringUtils.hasText(properties.getAccessKey())
                || !StringUtils.hasText(properties.getSecretKey()) || !StringUtils.hasText(properties.getRedirectUrl())
                || !StringUtils.hasText(properties.getIpnUrl())) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_PROVIDER_DISABLED);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
