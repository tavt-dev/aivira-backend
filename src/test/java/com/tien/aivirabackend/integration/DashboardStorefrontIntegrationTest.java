package com.tien.aivirabackend.integration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
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
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.OrderRepository;
import com.tien.aivirabackend.repository.PaymentGroupRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;

import tools.jackson.databind.JsonNode;

class DashboardStorefrontIntegrationTest extends AbstractIntegrationTest {
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

    @Test
    void storefrontAndDashboard_shouldUseExistingCommerceData() throws Exception {
        String adminToken = adminToken();
        User customer = saveCustomer();
        Category category = categoryRepository.save(Category.builder().categoryName("Fiction").slug("fiction")
                .description("Fiction books").displayOrder(0).active(true).visible(true).build());
        Product bestseller = saveBook(category, "BOOK-001", "Bestseller", "bestseller", 2, 15, true,
                ProductStatus.ACTIVE);
        saveBook(category, "BOOK-002", "Inactive Book", "inactive-book", 10, 1, true, ProductStatus.INACTIVE);
        savePaidOrder(customer, bestseller);
        String customerToken = token("buyer");

        mockMvc.perform(get("/orders").header("Authorization", "Bearer " + customerToken).param("page", "1")
                .param("size", "20")).andExpect(status().isOk()).andExpect(jsonPath("$.data.currentPage").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.data[0].orderCode").value("ORD-DASHBOARD"))
                .andExpect(jsonPath("$.data.data[0].paymentGroupCode").value("PAY-DASHBOARD"))
                .andExpect(jsonPath("$.data.data[0].itemCount").value(2));

        mockMvc.perform(get("/orders").header("Authorization", "Bearer " + customerToken)
                .param("status", "CANCELLED").param("page", "1").param("size", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.data").isEmpty());

        mockMvc.perform(get("/storefront/home")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featuredBooks[0].productName").value("Bestseller"))
                .andExpect(jsonPath("$.data.bestsellingBooks[0].productName").value("Bestseller"))
                .andExpect(jsonPath("$.data.categoryHighlights[0].categoryName").value("Fiction"));

        mockMvc.perform(get("/admin/dashboard/summary").header("Authorization", "Bearer " + adminToken)
                .param("fromDate", "2020-01-01T00:00:00Z").param("toDate", "2030-01-01T00:00:00Z"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.revenue").value(200.00))
                .andExpect(jsonPath("$.data.orderCount").value(1))
                .andExpect(jsonPath("$.data.successfulPaymentCount").value(1))
                .andExpect(jsonPath("$.data.lowStockCount").value(1));

        mockMvc.perform(get("/admin/dashboard/top-books").header("Authorization", "Bearer " + adminToken)
                .param("fromDate", "2020-01-01T00:00:00Z").param("toDate", "2030-01-01T00:00:00Z"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.books[0].productName").value("Bestseller"))
                .andExpect(jsonPath("$.data.books[0].quantitySold").value(2));

        mockMvc.perform(get("/admin/dashboard/low-stock").header("Authorization", "Bearer " + adminToken)
                .param("threshold", "5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.books[0].productName").value("Bestseller"));
    }

    private String adminToken() throws Exception {
        var role = roleRepository.findByCode(PredefinedRole.ADMIN).orElseThrow();
        User admin = User.builder().username("admin").email("admin@example.com")
                .password(passwordEncoder.encode(PASSWORD)).provider(SignInProvider.LOCAL).emailVerified(true)
                .isActive(true).isLocked(false).isDeleted(false).build();
        admin.getRoles().add(role);
        userRepository.save(admin);

        return token("admin");
    }

    private String token(String username) throws Exception {
        MvcResult login = mockMvc
                .perform(post("/auth/token").contentType(APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return read(login, "/data/token").asText();
    }

    private User saveCustomer() {
        return userRepository.save(
                User.builder().username("buyer").email("buyer@example.com").password(passwordEncoder.encode(PASSWORD))
                        .provider(SignInProvider.LOCAL).emailVerified(true).isActive(true).isDeleted(false).build());
    }

    private Product saveBook(Category category, String sku, String productName, String slug, int stockQuantity,
            int soldCount, boolean featured, ProductStatus status) {
        return productRepository.save(Product.builder().category(category).sku(sku).productName(productName).slug(slug)
                .description(productName).bookAuthor("Aivira").price(new BigDecimal("100.00"))
                .stockQuantity(stockQuantity).soldCount(soldCount).active(true).featured(featured).status(status)
                .build());
    }

    private void savePaidOrder(User customer, Product product) {
        PaymentGroup group = paymentGroupRepository
                .save(PaymentGroup.builder().paymentCode("PAY-DASHBOARD").user(customer).method(PaymentMethod.VNPAY)
                        .status(PaymentStatus.SUCCESS).amount(new BigDecimal("200.00")).build());
        Order order = Order.builder().orderCode("ORD-DASHBOARD").user(customer).subtotal(new BigDecimal("200.00"))
                .shippingFee(BigDecimal.ZERO).discountAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("200.00"))
                .orderStatus(OrderStatus.COMPLETED).shippingRecipientName("Buyer").shippingPhoneNumber("0900000000")
                .shippingAddressLine("123 Street").build();
        order.getItems()
                .add(OrderItem.builder().order(order).productId(product.getId()).productName(product.getProductName())
                        .sku(product.getSku()).thumbnailUrl(product.getThumbnailUrl())
                        .basePrice(new BigDecimal("100.00")).additionalPrice(BigDecimal.ZERO)
                        .discountAmount(BigDecimal.ZERO).finalPrice(new BigDecimal("100.00")).quantity(2).build());
        order.getPayments().add(Payment.builder().order(order).paymentGroup(group).method(PaymentMethod.VNPAY)
                .status(PaymentStatus.SUCCESS).amount(new BigDecimal("200.00")).paidAt(Instant.now()).build());
        orderRepository.save(order);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode read(MvcResult result, String pointer) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).at(pointer);
    }
}
