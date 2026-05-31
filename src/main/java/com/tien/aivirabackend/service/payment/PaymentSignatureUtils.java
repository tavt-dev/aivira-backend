package com.tien.aivirabackend.service.payment;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class PaymentSignatureUtils {
    private PaymentSignatureUtils() {}

    public static String hmacSha256(String data, String secret) {
        return hmac(data, secret, "HmacSHA256");
    }

    public static String hmacSha512(String data, String secret) {
        return hmac(data, secret, "HmacSHA512");
    }

    private static String hmac(String data, String secret, String algorithm) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot sign payment payload", ex);
        }
    }
}
