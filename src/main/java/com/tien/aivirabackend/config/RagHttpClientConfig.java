package com.tien.aivirabackend.config;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.tien.aivirabackend.config.properties.RagProperties;

@Configuration
public class RagHttpClientConfig {
    @Bean("qdrantRestClient")
    RestClient qdrantRestClient(RagProperties properties) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(properties.readTimeout());
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.qdrantUrl()).requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        if (properties.qdrantApiKey() != null && !properties.qdrantApiKey().isBlank())
            builder.defaultHeader("api-key", properties.qdrantApiKey());
        return builder.build();
    }
}

