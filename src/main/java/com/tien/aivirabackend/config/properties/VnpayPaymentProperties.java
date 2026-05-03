package com.tien.aivirabackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "payment.vnpay")
public class VnpayPaymentProperties {
    private boolean enabled = false;
    private String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String tmnCode = "";
    private String hashSecret = "";
    private String returnUrl = "";
    private String ipnUrl = "";
    private String transactionUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
}
