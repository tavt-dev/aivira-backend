package com.tien.aivirabackend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.data.domain.PageRequest;

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
import com.tien.aivirabackend.repository.NotificationRepository;
import com.tien.aivirabackend.repository.PaymentGroupRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ProductVariationRepository;
import com.tien.aivirabackend.repository.RefundRepository;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;

import tools.jackson.databind.JsonNode;

class AdminOrderIntegrationTest extends AbstractIntegrationTest {
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
    ProductVariationRepository productVariationRepository;

    @Autowired
    PaymentGroupRepository paymentGroupRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    RefundRepository refundRepository;

    @Autowired
    NotificationRepository notificationRepository;

    @Test
    void adminOrders_shouldListAndFilterByStatusAndKeyword() throws Exception {
        String token = adminToken();
        Order order = saveOrder(OrderStatus.PENDING_CONFIRMATION, PaymentStatus.PENDING, 3);

        mockMvc.perform(
                get("/admin/orders").header("Authorization", "Bearer " + token).param("status", "PENDING_CONFIRMATION"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.data[0].orderCode").value(order.getOrderCode()));

        mockMvc.perform(get("/admin/orders").header("Authorization", "Bearer " + token).param("keyword", "0900000000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.data[0].orderCode").value(order.getOrderCode()));
    }

    @Test
    void orderDetail_shouldReturnItemsAndPaymentForCustomerAndAdmin() throws Exception {
        String customerToken = userToken("buyer", "buyer@example.com", PredefinedRole.USER);
        User customer = userRepository.findByUsername("buyer").orElseThrow();
        Order order = saveOrderForCustomer(customer, OrderStatus.PENDING_CONFIRMATION, PaymentStatus.PENDING, 3,
                PaymentMethod.COD);
        String adminToken = adminToken();

        mockMvc.perform(get("/orders/{orderId}", order.getId()).header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].sku").value("BOOK-001-PB"))
                .andExpect(jsonPath("$.data.paymentGroupCode").value("PAY-PENDING_CONFIRMATION"))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"));

        mockMvc.perform(get("/admin/orders/{orderId}", order.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].sku").value("BOOK-001-PB"))
                .andExpect(jsonPath("$.data.paymentGroupCode").value("PAY-PENDING_CONFIRMATION"))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"));
    }

    @Test
    void adminCodLifecycle_shouldMovePendingConfirmationToCompleted() throws Exception {
        String token = adminToken();
        Order order = saveOrder(OrderStatus.PENDING_CONFIRMATION, PaymentStatus.PENDING, 3);

        mockMvc.perform(
                put("/admin/orders/{orderId}/confirm", order.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"));
        mockMvc.perform(
                put("/admin/orders/{orderId}/packing", order.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value("PACKING"));
        mockMvc.perform(
                put("/admin/orders/{orderId}/shipping", order.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value("SHIPPING"));
        mockMvc.perform(
                put("/admin/orders/{orderId}/completed", order.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value("COMPLETED"));

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getOrderStatus())
                .isEqualTo(OrderStatus.COMPLETED);
        var notifications = notificationRepository
                .findInbox(order.getUser().getId(), null, null, PageRequest.of(0, 20)).getContent();
        assertThat(notifications).extracting(notification -> notification.getType().name())
                .containsExactlyInAnyOrder("ORDER_COMPLETED", "ORDER_SHIPPING", "ORDER_PACKING", "ORDER_CONFIRMED");

        String customerToken = loginExistingUser("buyer");
        mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(4));
        mockMvc.perform(get("/notifications/unread-count").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.unreadCount").value(4));
        mockMvc.perform(patch("/notifications/{id}/read", notifications.getFirst().getId())
                .header("Authorization", "Bearer " + customerToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));
    }

    @Test
    void adminCancelBeforeShipping_shouldRestoreStock() throws Exception {
        String token = adminToken();
        Order order = saveOrder(OrderStatus.CONFIRMED, PaymentStatus.PENDING, 3);
        Long variationId = order.getItems().getFirst().getProductVariationId();

        mockMvc.perform(put("/admin/orders/{orderId}/cancel", order.getId()).header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON).content(json(Map.of("reason", "out of stock"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelReason").value("out of stock"));

        Long paymentGroupId = order.getPayments().getFirst().getPaymentGroup().getId();
        assertThat(paymentGroupRepository.findById(paymentGroupId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELLED);
        assertThat(productVariationRepository.findById(variationId).orElseThrow().getStockQuantity()).isEqualTo(5);
    }

    @Test
    void adminManualRefund_shouldMarkPaidOrderRefundedAndRestoreStock() throws Exception {
        String token = adminToken();
        Order order = saveOrder(OrderStatus.PAID, PaymentStatus.SUCCESS, 3, PaymentMethod.VNPAY);
        Long variationId = order.getItems().getFirst().getProductVariationId();

        mockMvc.perform(put("/admin/orders/{orderId}/mark-refunded", order.getId())
                .header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON)
                .content(json(Map.of("amount", "200.00", "reason", "customer refund", "note",
                        "manual bank transfer completed"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value("REFUNDED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refund.amount").value(200.00))
                .andExpect(jsonPath("$.data.refund.refundCode").exists());

        Order refundedOrder = orderRepository.findWithPaymentsById(order.getId()).orElseThrow();
        assertThat(refundedOrder.getOrderStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(refundedOrder.getPayments().getFirst().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(paymentGroupRepository.findById(refundedOrder.getPayments().getFirst().getPaymentGroup().getId())
                .orElseThrow().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(productVariationRepository.findById(variationId).orElseThrow().getStockQuantity()).isEqualTo(5);
        assertThat(refundRepository.existsByOrder_Id(order.getId())).isTrue();

        mockMvc.perform(put("/admin/orders/{orderId}/mark-refunded", order.getId())
                .header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON)
                .content(json(Map.of("amount", "200.00", "reason", "duplicate", "note", "duplicate manual refund"))))
                .andExpect(status().isBadRequest());
    }

    private String adminToken() throws Exception {
        return userToken("admin", "admin@example.com", PredefinedRole.ADMIN);
    }

    private String userToken(String username, String email, PredefinedRole roleCode) throws Exception {
        var role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = User.builder().username(username).email(email).password(passwordEncoder.encode(PASSWORD))
                .provider(SignInProvider.LOCAL).emailVerified(true).isActive(true).isLocked(false).isDeleted(false)
                .build();
        user.getRoles().add(role);
        userRepository.save(user);

        MvcResult login = mockMvc
                .perform(post("/auth/token").contentType(APPLICATION_JSON)
                        .header("User-Agent", "integration-test")
                        .content(json(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return read(login, "/data/token").asText();
    }

    private String loginExistingUser(String username) throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/token").contentType(APPLICATION_JSON)
                .header("User-Agent", "integration-test")
                .content(json(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return read(login, "/data/token").asText();
    }

    private Order saveOrder(OrderStatus orderStatus, PaymentStatus paymentStatus, int variationStock) {
        return saveOrder(orderStatus, paymentStatus, variationStock, PaymentMethod.COD);
    }

    private Order saveOrder(OrderStatus orderStatus, PaymentStatus paymentStatus, int variationStock,
            PaymentMethod paymentMethod) {
        User customer = userRepository.save(
                User.builder().username("buyer").email("buyer@example.com").password(passwordEncoder.encode(PASSWORD))
                        .provider(SignInProvider.LOCAL).emailVerified(true).isActive(true).isLocked(false)
                        .isDeleted(false).build());
        return saveOrderForCustomer(customer, orderStatus, paymentStatus, variationStock, paymentMethod);
    }

    private Order saveOrderForCustomer(User customer, OrderStatus orderStatus, PaymentStatus paymentStatus,
            int variationStock, PaymentMethod paymentMethod) {
        Category category = categoryRepository.save(Category.builder().categoryName("Books").slug("books")
                .description("Books").displayOrder(0).active(true).visible(true).build());
        Product product = Product.builder().category(category).sku("BOOK-001").productName("Aivira Book")
                .slug("aivira-book").description("Book").bookAuthor("Aivira").price(BigDecimal.valueOf(100))
                .stockQuantity(variationStock).soldCount(0).active(true).featured(false).status(ProductStatus.ACTIVE)
                .build();
        ProductVariation variation = ProductVariation.builder().product(product).sku("BOOK-001-PB").color("Default")
                .size("Paperback").additionalPrice(BigDecimal.ZERO).stockQuantity(variationStock).active(true).build();
        product.getProductVariations().add(variation);
        Product savedProduct = productRepository.save(product);
        ProductVariation savedVariation = savedProduct.getProductVariations().iterator().next();

        PaymentGroup group = paymentGroupRepository.save(PaymentGroup.builder().paymentCode("PAY-" + orderStatus.name())
                .user(customer).method(paymentMethod).status(paymentStatus).amount(new BigDecimal("200.00")).build());
        Order order = Order.builder().orderCode("ORD-" + orderStatus.name()).user(customer)
                .subtotal(new BigDecimal("200.00")).shippingFee(BigDecimal.ZERO).discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("200.00")).orderStatus(orderStatus).shippingRecipientName("Buyer")
                .shippingPhoneNumber("0900000000").shippingAddressLine("123 Street").build();
        order.getItems().add(OrderItem.builder().order(order).productId(savedProduct.getId())
                .productVariationId(savedVariation.getId()).productName(savedProduct.getProductName())
                .sku(savedVariation.getSku()).basePrice(new BigDecimal("100.00")).additionalPrice(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO).finalPrice(new BigDecimal("100.00")).quantity(2).build());
        order.getPayments().add(Payment.builder().order(order).paymentGroup(group).method(paymentMethod)
                .status(paymentStatus).amount(new BigDecimal("200.00")).build());
        return orderRepository.save(order);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode read(MvcResult result, String pointer) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).at(pointer);
    }
}
