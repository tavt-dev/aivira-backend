package com.tien.aivirabackend.integration;

import jakarta.servlet.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tien.aivirabackend.service.CloudinaryStorageService;
import com.tien.aivirabackend.service.EmailService;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("aivira_test")
            .withUsername("test")
            .withPassword("test");

    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    Filter springSecurityFilterChain;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @MockitoBean
    protected EmailService emailService;

    @MockitoBean
    protected CloudinaryStorageService cloudinaryStorageService;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @BeforeEach
    void setUpIntegrationTest() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE review_images");
        jdbcTemplate.execute("TRUNCATE TABLE reviews");
        jdbcTemplate.execute("TRUNCATE TABLE coupon_usages");
        jdbcTemplate.execute("TRUNCATE TABLE payments");
        jdbcTemplate.execute("TRUNCATE TABLE order_items");
        jdbcTemplate.execute("TRUNCATE TABLE orders");
        jdbcTemplate.execute("TRUNCATE TABLE cart_items");
        jdbcTemplate.execute("TRUNCATE TABLE carts");
        jdbcTemplate.execute("TRUNCATE TABLE product_media");
        jdbcTemplate.execute("TRUNCATE TABLE product_variations");
        jdbcTemplate.execute("TRUNCATE TABLE products");
        jdbcTemplate.execute("TRUNCATE TABLE categories");
        jdbcTemplate.execute("TRUNCATE TABLE user_permissions");
        jdbcTemplate.execute("TRUNCATE TABLE user_roles");
        jdbcTemplate.execute("TRUNCATE TABLE shops");
        jdbcTemplate.execute("TRUNCATE TABLE addresses");
        jdbcTemplate.execute("TRUNCATE TABLE user_otp");
        jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens");
        jdbcTemplate.execute("TRUNCATE TABLE invalidated_tokens");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
