package com.tien.aivirabackend.service.payment;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.tien.aivirabackend.config.properties.MomoPaymentProperties;
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
public class MomoPaymentProviderClient implements PaymentProviderClient {
    MomoPaymentProperties properties;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.MOMO;
    }

    @Override
    public PaymentProviderResult createPayment(PaymentGroup paymentGroup) {
        requireEnabled();
        String requestId = paymentGroup.getPaymentCode() + "-" + System.currentTimeMillis();
        String orderId = paymentGroup.getPaymentCode();
        String amount = paymentGroup.getAmount().toBigInteger().toString();
        String extraData = "";
        String orderInfo = "Thanh toan " + paymentGroup.getPaymentCode();
        String rawSignature = "accessKey=" + properties.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + properties.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + properties.getPartnerCode()
                + "&redirectUrl=" + properties.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=" + properties.getRequestType();
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

        Map<String, Object> response = RestClient.create()
                .post()
                .uri(properties.getEndpoint())
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (response == null) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_PROVIDER_ERROR);
        }
        return new PaymentProviderResult(
                requestId,
                stringValue(response.get("transId")),
                stringValue(response.get("payUrl")),
                stringValue(response.get("deeplink")),
                stringValue(response.get("qrCodeUrl")),
                response.toString());
    }

    @Override
    public boolean verifyCallback(Map<String, ?> payload) {
        requireEnabled();
        Object signature = payload.get("signature");
        if (signature == null) {
            return false;
        }
        String rawSignature = "accessKey=" + properties.getAccessKey()
                + "&amount=" + stringValue(payload.get("amount"))
                + "&extraData=" + stringValue(payload.get("extraData"))
                + "&message=" + stringValue(payload.get("message"))
                + "&orderId=" + stringValue(payload.get("orderId"))
                + "&orderInfo=" + stringValue(payload.get("orderInfo"))
                + "&orderType=" + stringValue(payload.get("orderType"))
                + "&partnerClientId=" + stringValue(payload.get("partnerClientId"))
                + "&partnerCode=" + stringValue(payload.get("partnerCode"))
                + "&payType=" + stringValue(payload.get("payType"))
                + "&requestId=" + stringValue(payload.get("requestId"))
                + "&responseTime=" + stringValue(payload.get("responseTime"))
                + "&resultCode=" + stringValue(payload.get("resultCode"))
                + "&transId=" + stringValue(payload.get("transId"));
        return PaymentSignatureUtils.hmacSha256(rawSignature, properties.getSecretKey())
                .equalsIgnoreCase(String.valueOf(signature));
    }

    private void requireEnabled() {
        if (!properties.isEnabled()
                || !StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getPartnerCode())
                || !StringUtils.hasText(properties.getAccessKey())
                || !StringUtils.hasText(properties.getSecretKey())
                || !StringUtils.hasText(properties.getRedirectUrl())
                || !StringUtils.hasText(properties.getIpnUrl())) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_PROVIDER_DISABLED);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
