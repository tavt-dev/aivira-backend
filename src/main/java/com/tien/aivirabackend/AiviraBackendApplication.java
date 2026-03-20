package com.tien.aivirabackend;

import com.tien.aivirabackend.config.properties.FileUploadProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({FileUploadProperties.class })
public class AiviraBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiviraBackendApplication.class, args);
    }
}
