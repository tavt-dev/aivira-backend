package com.tien.aivirabackend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.constant.SignInProvider;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.OrderRepository;
import com.tien.aivirabackend.repository.PaymentGroupRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ReviewRepository;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;

import tools.jackson.databind.JsonNode;

class ReviewIntegrationTest extends AbstractIntegrationTest {
    private static final String PASSWORD = "Password123!";

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    PaymentGroupRepository paymentGroupRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ReviewRepository reviewRepository;

    @Test
    void reviewFlow_shouldRequireApprovalAndSoftDelete() throws Exception {
        String customerToken = userToken("buyer", "buyer@example.com", PredefinedRole.USER);
        String adminToken = userToken("admin", "admin@example.com", PredefinedRole.ADMIN);
        Order order = saveCompletedOrder("buyer");
        Long orderItemId = order.getItems().getFirst().getId();

        MvcResult create = mockMvc.perform(
                        post("/orders/{orderId}/items/{orderItemId}/review", order.getId(), orderItemId)
                                .header("Authorization", "Bearer " + customerToken)
                                .contentType(APPLICATION_JSON)
                                .content(json(Map.of(
                                        "rating",
                                        5,
                                        "comment",
                                        "Great book",
                                        "images",
                                        java.util.List.of(Map.of(
                                                "imageUrl", "https://cdn.example.com/review.jpg",
                                                "imagePublicId", "review-img",
                                                "sortOrder", 0))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approved").value(false))
                .andReturn();
        Long reviewId = read(create, "/data/id").asLong();

        mockMvc.perform(get("/products/aivira-book/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(put("/admin/reviews/{reviewId}/moderate", reviewId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("approved", true, "visible", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approved").value(true));
        mockMvc.perform(put("/admin/reviews/{reviewId}/reply", reviewId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("adminReply", "Thanks for the review"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminReply").value("Thanks for the review"));

        mockMvc.perform(get("/products/aivira-book/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.data[0].adminReply").value("Thanks for the review"));

        mockMvc.perform(post("/orders/{orderId}/items/{orderItemId}/review", order.getId(), orderItemId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("rating", 4, "comment", "duplicate"))))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/reviews/{reviewId}", reviewId).header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products/aivira-book/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
        assertThat(reviewRepository.findById(reviewId).orElseThrow().getDeletedAt())
                .isNotNull();
    }

    private String userToken(String username, String email, PredefinedRole roleCode) throws Exception {
        var role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .provider(SignInProvider.LOCAL)
                .emailVerified(true)
                .isActive(true)
                .isLocked(false)
                .isDeleted(false)
                .build();
        user.getRoles().add(role);
        userRepository.save(user);

        MvcResult login = mockMvc.perform(post("/auth/token")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return read(login, "/data/token").asText();
    }

    private Order saveCompletedOrder(String username) {
        User customer = userRepository.findByUsername(username).orElseThrow();
        Category category = categoryRepository.save(Category.builder()
                .categoryName("Books")
                .slug("books")
                .description("Books")
                .displayOrder(0)
                .active(true)
                .visible(true)
                .build());
        Product product = Product.builder()
                .category(category)
                .sku("BOOK-001")
                .productName("Aivira Book")
                .slug("aivira-book")
                .description("Book")
                .bookAuthor("Aivira")
                .price(BigDecimal.valueOf(100))
                .stockQuantity(3)
                .soldCount(1)
                .active(true)
                .featured(false)
                .status(ProductStatus.ACTIVE)
                .build();
        ProductVariation variation = ProductVariation.builder()
                .product(product)
                .sku("BOOK-001-PB")
                .color("Default")
                .size("Paperback")
                .additionalPrice(BigDecimal.ZERO)
                .stockQuantity(3)
                .active(true)
                .build();
        product.getProductVariations().add(variation);
        Product savedProduct = productRepository.save(product);
        ProductVariation savedVariation =
                savedProduct.getProductVariations().iterator().next();

        PaymentGroup group = paymentGroupRepository.save(PaymentGroup.builder()
                .paymentCode("PAY-REVIEW")
                .user(customer)
                .method(PaymentMethod.COD)
                .status(PaymentStatus.SUCCESS)
                .amount(new BigDecimal("100.00"))
                .build());
        Order order = Order.builder()
                .orderCode("ORD-REVIEW")
                .user(customer)
                .subtotal(new BigDecimal("100.00"))
                .shippingFee(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100.00"))
                .orderStatus(OrderStatus.COMPLETED)
                .shippingRecipientName("Buyer")
                .shippingPhoneNumber("0900000000")
                .shippingAddressLine("123 Street")
                .build();
        order.getItems()
                .add(OrderItem.builder()
                        .order(order)
                        .productId(savedProduct.getId())
                        .productVariationId(savedVariation.getId())
                        .productName(savedProduct.getProductName())
                        .sku(savedVariation.getSku())
                        .basePrice(new BigDecimal("100.00"))
                        .additionalPrice(BigDecimal.ZERO)
                        .discountAmount(BigDecimal.ZERO)
                        .finalPrice(new BigDecimal("100.00"))
                        .quantity(1)
                        .build());
        order.getPayments()
                .add(Payment.builder()
                        .order(order)
                        .paymentGroup(group)
                        .method(PaymentMethod.COD)
                        .status(PaymentStatus.SUCCESS)
                        .amount(new BigDecimal("100.00"))
                        .build());
        return orderRepository.save(order);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode read(MvcResult result, String pointer) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).at(pointer);
    }
}
