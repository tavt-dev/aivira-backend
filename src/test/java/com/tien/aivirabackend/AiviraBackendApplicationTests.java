package com.tien.aivirabackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:aivira_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.signerKey=test-secret-key-with-at-least-32-characters",
    "cloudinary.cloud-name=test-cloud",
    "cloudinary.api-key=test-key",
    "cloudinary.api-secret=test-secret"
})
class AiviraBackendApplicationTests {

    @Test
    void contextLoads() {}
}
