package com.tien.aivirabackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tien.aivirabackend.config.properties.CloudinaryProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CloudinaryConfig {
    private final CloudinaryProperties properties;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap("cloud_name", properties.getCloudName(), "api_key",
                properties.getApiKey(), "api_secret", properties.getApiSecret(), "secure", properties.isSecure()));
    }
}
