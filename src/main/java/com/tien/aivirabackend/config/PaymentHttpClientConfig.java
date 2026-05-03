package com.tien.aivirabackend.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.tien.aivirabackend.config.properties.PaymentProperties;

@Configuration
public class PaymentHttpClientConfig {
    @Bean
    RestClient paymentRestClient(PaymentProperties paymentProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(paymentProperties.getProviderConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(paymentProperties.getProviderReadTimeoutMs()));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
