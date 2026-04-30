package com.tien.aivirabackend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ShopRepository;
import com.tien.aivirabackend.repository.UserRepository;

class CatalogPublicIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    UserRepository userRepository;

    @Autowired
    ShopRepository shopRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Test
    void publicCatalogEndpoints_shouldExposeOnlyVisibleActiveProductsWithoutToken() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .categoryName("Fashion")
                .slug("fashion")
                .description("Fashion")
                .displayOrder(0)
                .active(true)
                .visible(true)
                .build());
        Shop approvedShop = shopRepository.save(buildShop("seller-1", "aivira-fashion", ShopStatus.APPROVED));
        Shop lockedShop = shopRepository.save(buildShop("seller-2", "locked-fashion", ShopStatus.LOCKED));
        saveProduct("ACTIVE-1", "active-product", ProductStatus.ACTIVE, approvedShop, category);
        saveProduct("DRAFT-1", "draft-product", ProductStatus.DRAFT, approvedShop, category);
        saveProduct("LOCKED-1", "locked-shop-product", ProductStatus.ACTIVE, lockedShop, category);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("fashion"));

        mockMvc.perform(get("/products").param("keyword", "product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.data[0].slug").value("active-product"));

        mockMvc.perform(get("/products/{slug}", "active-product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/products/{slug}", "draft-product")).andExpect(status().isNotFound());
    }

    @Test
    void sellerCatalogEndpoints_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/seller/products")).andExpect(status().isUnauthorized());
    }

    private Shop buildShop(String username, String slug, ShopStatus status) {
        User owner = userRepository.save(User.builder()
                .username(username)
                .email(username + "@example.com")
                .emailVerified(true)
                .isActive(true)
                .build());
        return Shop.builder()
                .owner(owner)
                .shopName(slug)
                .slug(slug)
                .businessEmail(username + "@example.com")
                .phoneNumber("0900000000")
                .legalName("Aivira LLC")
                .pickupAddressLine("123 Street")
                .pickupCity("Ho Chi Minh")
                .status(status)
                .build();
    }

    private void saveProduct(String sku, String slug, ProductStatus status, Shop shop, Category category) {
        Product product = Product.builder()
                .shop(shop)
                .category(category)
                .sku(sku)
                .productName(slug)
                .slug(slug)
                .description("Product description")
                .price(BigDecimal.valueOf(100))
                .stockQuantity(5)
                .soldCount(0)
                .active(true)
                .featured(false)
                .status(status)
                .build();
        ProductVariation variation = ProductVariation.builder()
                .product(product)
                .sku(sku + "-VAR")
                .color("Black")
                .size("M")
                .additionalPrice(BigDecimal.ZERO)
                .stockQuantity(5)
                .active(true)
                .build();
        product.getProductVariations().add(variation);
        productRepository.save(product);
    }
}
