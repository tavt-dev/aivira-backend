package com.tien.aivirabackend.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.tien.aivirabackend.config.properties.MomoPaymentProperties;
import com.tien.aivirabackend.config.properties.VnpayPaymentProperties;
import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentProvider;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.dto.RequestMetadata;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.service.payment.provider.MomoPaymentProviderClient;
import com.tien.aivirabackend.service.payment.provider.PaymentProviderCallbackResult;
import com.tien.aivirabackend.service.payment.provider.PaymentProviderQueryResult;
import com.tien.aivirabackend.service.payment.provider.PaymentProviderRequest;
import com.tien.aivirabackend.service.payment.provider.PaymentProviderResult;
import com.tien.aivirabackend.service.payment.provider.VnpayPaymentProviderClient;

class PaymentProviderClientTest {
    @Test
    void vnpayCreatePaymentBuildsSignedUrlWithAmountTimes100() {
        VnpayPaymentProviderClient client = new VnpayPaymentProviderClient(vnpayProperties(), RestClient.create());
        PaymentGroup group = paymentGroup(PaymentMethod.VNPAY);
        PaymentAttempt attempt = paymentAttempt(group, PaymentProvider.VNPAY, "PAY123-A1-ABC");

        PaymentProviderResult result = client.createPayment(
                new PaymentProviderRequest(group, attempt, new RequestMetadata("JUnit", "10.0.0.1")));

        assertThat(result.providerTxnRef()).isEqualTo("PAY123-A1-ABC");
        assertThat(result.paymentUrl()).contains("vnp_Amount=123400");
        assertThat(result.paymentUrl()).contains("vnp_IpAddr=10.0.0.1");
        assertThat(result.paymentUrl()).contains("vnp_SecureHash=");
    }

    @Test
    void vnpayVerifyCallbackIgnoresSecureHashType() {
        VnpayPaymentProviderClient client = new VnpayPaymentProviderClient(vnpayProperties(), RestClient.create());
        Map<String, String> payload = new TreeMap<>();
        payload.put("vnp_Amount", "123400");
        payload.put("vnp_ResponseCode", "00");
        payload.put("vnp_TmnCode", "TESTTMN");
        payload.put("vnp_TransactionNo", "14123456");
        payload.put("vnp_TransactionStatus", "00");
        payload.put("vnp_TxnRef", "PAY123-A1-ABC");
        payload.put("vnp_SecureHashType", "HmacSHA512");
        payload.put("vnp_SecureHash", PaymentSignatureUtils.hmacSha512(vnpayQuery(payload), "secret"));

        assertThat(client.verifyCallback(payload)).isTrue();
        PaymentProviderCallbackResult result = client.parseCallback(payload);
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.amount()).isEqualByComparingTo("1234");
    }

    @Test
    void momoVerifyCallbackUsesExpectedFieldOrder() {
        MomoPaymentProviderClient client = new MomoPaymentProviderClient(momoProperties(), RestClient.create());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", "MOMO");
        payload.put("orderId", "PAY123-A1-ABC");
        payload.put("requestId", "PAY123-A1-ABC-REQ");
        payload.put("amount", 1234);
        payload.put("orderInfo", "Thanh toan PAY123");
        payload.put("orderType", "momo_wallet");
        payload.put("transId", 987654321);
        payload.put("resultCode", 0);
        payload.put("message", "Successful.");
        payload.put("payType", "qr");
        payload.put("responseTime", 1710000000000L);
        payload.put("extraData", "");
        payload.put("partnerClientId", "");
        payload.put("signature", momoIpnSignature(payload));

        assertThat(client.verifyCallback(payload)).isTrue();
        PaymentProviderCallbackResult result = client.parseCallback(payload);
        assertThat(result.providerTxnRef()).isEqualTo("PAY123-A1-ABC");
        assertThat(result.requestId()).isEqualTo("PAY123-A1-ABC-REQ");
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void momoQueryPayment_shouldParseProviderStatusWithoutCallingRealSandbox() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MomoPaymentProviderClient client = new MomoPaymentProviderClient(momoProperties(), builder.build());
        PaymentGroup group = paymentGroup(PaymentMethod.MOMO);
        PaymentAttempt attempt = paymentAttempt(group, PaymentProvider.MOMO, "PAY123-A1-ABC");

        server.expect(once(), requestTo("http://localhost/momo/query"))
                .andRespond(withSuccess(
                        """
						{"resultCode":0,"message":"Successful.","amount":1234,"transId":987654321}
						""",
                        MediaType.APPLICATION_JSON));

        PaymentProviderQueryResult result = client.queryPayment(attempt);

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.amount()).isEqualByComparingTo("1234");
        assertThat(result.transactionId()).isEqualTo("987654321");
        server.verify();
    }

    @Test
    void vnpayQueryPayment_shouldParseProviderStatusWithoutCallingRealSandbox() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        VnpayPaymentProviderClient client = new VnpayPaymentProviderClient(vnpayProperties(), builder.build());
        PaymentGroup group = paymentGroup(PaymentMethod.VNPAY);
        PaymentAttempt attempt = paymentAttempt(group, PaymentProvider.VNPAY, "PAY123-A1-ABC");

        server.expect(once(), requestTo("http://localhost/vnpay/query"))
                .andRespond(withSuccess(
                        """
						{"vnp_TransactionStatus":"00","vnp_Message":"Confirm Success","vnp_Amount":"123400","vnp_TransactionNo":"14123456"}
						""",
                        MediaType.APPLICATION_JSON));

        PaymentProviderQueryResult result = client.queryPayment(attempt);

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.amount()).isEqualByComparingTo("1234");
        assertThat(result.transactionId()).isEqualTo("14123456");
        server.verify();
    }

    @Test
    void vnpayCreatePayment_whenProviderDisabled_shouldThrowAppException() {
        VnpayPaymentProperties properties = vnpayProperties();
        properties.setEnabled(false);
        VnpayPaymentProviderClient client = new VnpayPaymentProviderClient(properties, RestClient.create());
        PaymentGroup group = paymentGroup(PaymentMethod.VNPAY);
        PaymentAttempt attempt = paymentAttempt(group, PaymentProvider.VNPAY, "PAY123-A1-ABC");

        assertThatThrownBy(() -> client.createPayment(
                        new PaymentProviderRequest(group, attempt, new RequestMetadata("JUnit", "10.0.0.1"))))
                .isInstanceOf(AppException.class);
    }

    private VnpayPaymentProperties vnpayProperties() {
        VnpayPaymentProperties properties = new VnpayPaymentProperties();
        properties.setEnabled(true);
        properties.setPaymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        properties.setTmnCode("TESTTMN");
        properties.setHashSecret("secret");
        properties.setReturnUrl("http://localhost/vnpay/return");
        properties.setIpnUrl("http://localhost/vnpay/ipn");
        properties.setTransactionUrl("http://localhost/vnpay/query");
        return properties;
    }

    private MomoPaymentProperties momoProperties() {
        MomoPaymentProperties properties = new MomoPaymentProperties();
        properties.setEnabled(true);
        properties.setEndpoint("http://localhost/momo/create");
        properties.setQueryEndpoint("http://localhost/momo/query");
        properties.setPartnerCode("MOMO");
        properties.setAccessKey("access");
        properties.setSecretKey("secret");
        properties.setRedirectUrl("http://localhost/momo/return");
        properties.setIpnUrl("http://localhost/momo/ipn");
        return properties;
    }

    private PaymentGroup paymentGroup(PaymentMethod method) {
        return PaymentGroup.builder()
                .paymentCode("PAY123")
                .method(method)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("1234.00"))
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
    }

    private PaymentAttempt paymentAttempt(PaymentGroup group, PaymentProvider provider, String providerTxnRef) {
        return PaymentAttempt.builder()
                .paymentGroup(group)
                .provider(provider)
                .method(group.getMethod())
                .attemptNo(1)
                .providerTxnRef(providerTxnRef)
                .requestId(providerTxnRef + "-REQ")
                .status(PaymentStatus.PENDING)
                .amount(group.getAmount())
                .expiresAt(group.getExpiresAt())
                .build();
    }

    private String vnpayQuery(Map<String, String> payload) {
        return payload.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("vnp_"))
                .filter(entry -> !"vnp_SecureHash".equals(entry.getKey()))
                .filter(entry -> !"vnp_SecureHashType".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String momoIpnSignature(Map<String, Object> payload) {
        String rawSignature = "accessKey=access"
                + "&amount=" + payload.get("amount")
                + "&extraData=" + payload.get("extraData")
                + "&message=" + payload.get("message")
                + "&orderId=" + payload.get("orderId")
                + "&orderInfo=" + payload.get("orderInfo")
                + "&orderType=" + payload.get("orderType")
                + "&partnerClientId=" + payload.get("partnerClientId")
                + "&partnerCode=" + payload.get("partnerCode")
                + "&payType=" + payload.get("payType")
                + "&requestId=" + payload.get("requestId")
                + "&responseTime=" + payload.get("responseTime")
                + "&resultCode=" + payload.get("resultCode")
                + "&transId=" + payload.get("transId");
        return PaymentSignatureUtils.hmacSha256(rawSignature, "secret");
    }
}
