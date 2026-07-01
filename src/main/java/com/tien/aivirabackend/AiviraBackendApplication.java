package com.tien.aivirabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.tien.aivirabackend.config.properties.BrevoProperties;
import com.tien.aivirabackend.config.properties.CloudinaryProperties;
import com.tien.aivirabackend.config.properties.FileUploadProperties;
import com.tien.aivirabackend.config.properties.GoogleOAuthProperties;
import com.tien.aivirabackend.config.properties.MomoPaymentProperties;
import com.tien.aivirabackend.config.properties.PaymentProperties;
import com.tien.aivirabackend.config.properties.VnpayPaymentProperties;

@SpringBootApplication
@EnableConfigurationProperties({
    BrevoProperties.class,
    FileUploadProperties.class,
    CloudinaryProperties.class,
    GoogleOAuthProperties.class,
    PaymentProperties.class,
    VnpayPaymentProperties.class,
    MomoPaymentProperties.class
})
public class AiviraBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiviraBackendApplication.class, args);
    }
}
