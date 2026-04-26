package com.tien.aivirabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.tien.aivirabackend.config.properties.CloudinaryProperties;
import com.tien.aivirabackend.config.properties.FileUploadProperties;

@SpringBootApplication
@EnableConfigurationProperties({FileUploadProperties.class, CloudinaryProperties.class})
public class AiviraBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiviraBackendApplication.class, args);
    }
}
