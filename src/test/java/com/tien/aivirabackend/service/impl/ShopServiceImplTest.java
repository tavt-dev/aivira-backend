package com.tien.aivirabackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.dto.request.ApplyShopRequest;
import com.tien.aivirabackend.domain.dto.request.UpdateShopRequest;
import com.tien.aivirabackend.domain.dto.response.ShopResponse;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.ShopMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.repository.ShopRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.CloudinaryStorageService;
import com.tien.aivirabackend.service.FileValidatorService;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {
    @Mock
    ShopRepository shopRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ShopMapper shopMapper;

    @Mock
    FileValidatorService fileValidatorService;

    @Mock
    CloudinaryStorageService cloudinaryStorageService;

    @InjectMocks
    ShopServiceImpl shopService;

    @BeforeEach
    void setUp() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("user_id", "user-1", "sub", "alice"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void apply_shouldCreatePendingShopWithUniqueSlug() {
        User user = buildUser();
        ApplyShopRequest request = ApplyShopRequest.builder()
                .shopName("Aivira Fashion")
                .businessEmail("shop@example.com")
                .phoneNumber("0900000000")
                .legalName("Aivira Fashion LLC")
                .pickupAddressLine("123 Street")
                .pickupCity("Ho Chi Minh")
                .build();
        ShopResponse response =
                ShopResponse.builder().id(1L).status(ShopStatus.PENDING).build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(shopRepository.existsByOwnerId("user-1")).thenReturn(false);
        when(shopRepository.existsBySlug("aivira-fashion")).thenReturn(false);
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> {
            Shop shop = invocation.getArgument(0);
            shop.setId(1L);
            return shop;
        });
        when(shopMapper.toShopResponse(any(Shop.class))).thenReturn(response);

        ShopResponse result = shopService.apply(request);

        assertThat(result).isSameAs(response);
        ArgumentCaptor<Shop> shopCaptor = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository).save(shopCaptor.capture());
        assertThat(shopCaptor.getValue().getOwner()).isSameAs(user);
        assertThat(shopCaptor.getValue().getSlug()).isEqualTo("aivira-fashion");
        assertThat(shopCaptor.getValue().getStatus()).isEqualTo(ShopStatus.PENDING);
    }

    @Test
    void apply_shouldRejectSecondShopForSameUser() {
        User user = buildUser();
        ApplyShopRequest request = ApplyShopRequest.builder()
                .shopName("Aivira Fashion")
                .businessEmail("shop@example.com")
                .phoneNumber("0900000000")
                .legalName("Aivira Fashion LLC")
                .pickupAddressLine("123 Street")
                .pickupCity("Ho Chi Minh")
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(shopRepository.existsByOwnerId("user-1")).thenReturn(true);

        assertThatThrownBy(() -> shopService.apply(request)).isInstanceOf(AppException.class);
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    void resubmitMyShop_shouldMoveRejectedShopToPending() {
        Shop shop = buildShop(ShopStatus.REJECTED);
        shop.setRejectionReason("Missing info");
        ShopResponse response =
                ShopResponse.builder().id(1L).status(ShopStatus.PENDING).build();

        when(shopRepository.findWithOwnerByOwnerId("user-1")).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shopMapper.toShopResponse(any(Shop.class))).thenReturn(response);

        ShopResponse result = shopService.resubmitMyShop();

        assertThat(result).isSameAs(response);
        assertThat(shop.getStatus()).isEqualTo(ShopStatus.PENDING);
        assertThat(shop.getRejectionReason()).isNull();
    }

    @Test
    void updateMyShop_shouldRejectLockedShop() {
        Shop shop = buildShop(ShopStatus.LOCKED);
        when(shopRepository.findWithOwnerByOwnerId("user-1")).thenReturn(Optional.of(shop));

        assertThatThrownBy(() -> shopService.updateMyShop(
                        UpdateShopRequest.builder().shopName("New Name").build()))
                .isInstanceOf(AppException.class);
        verify(shopRepository, never()).save(any(Shop.class));
    }

    private User buildUser() {
        User user = new User();
        user.setId("user-1");
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        return user;
    }

    private Shop buildShop(ShopStatus status) {
        return Shop.builder()
                .id(1L)
                .owner(buildUser())
                .shopName("Aivira Fashion")
                .slug("aivira-fashion")
                .businessEmail("shop@example.com")
                .phoneNumber("0900000000")
                .legalName("Aivira Fashion LLC")
                .pickupAddressLine("123 Street")
                .pickupCity("Ho Chi Minh")
                .status(status)
                .build();
    }
}
