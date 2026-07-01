package com.tien.aivirabackend.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.tien.aivirabackend.config.properties.BrevoProperties;

@Configuration
public class EmailHttpClientConfig {
    @Bean
    RestClient brevoRestClient(BrevoProperties brevoProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(brevoProperties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(brevoProperties.getReadTimeoutMs()));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
