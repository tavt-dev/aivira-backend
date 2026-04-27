package com.tien.aivirabackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.dto.request.ShopModerationRequest;
import com.tien.aivirabackend.domain.dto.response.ShopResponse;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.ShopMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.ShopRepository;
import com.tien.aivirabackend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminShopServiceImplTest {
    @Mock
    ShopRepository shopRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    ShopMapper shopMapper;

    @InjectMocks
    AdminShopServiceImpl adminShopService;

    @BeforeEach
    void setUp() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("user_id", "admin-1", "sub", "admin"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approve_shouldAssignSellerRoleAndMarkApproved() {
        User owner = buildOwner();
        Shop shop = buildShop(owner, ShopStatus.PENDING);
        Role sellerRole = Role.builder().id(2L).code(PredefinedRole.SELLER).build();
        ShopResponse response =
                ShopResponse.builder().id(1L).status(ShopStatus.APPROVED).build();

        when(shopRepository.findWithOwnerById(1L)).thenReturn(Optional.of(shop));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(owner));
        when(roleRepository.findByCode(PredefinedRole.SELLER)).thenReturn(Optional.of(sellerRole));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shopMapper.toShopResponse(any(Shop.class))).thenReturn(response);

        ShopResponse result = adminShopService.approve(1L);

        assertThat(result).isSameAs(response);
        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
        assertThat(shop.getApprovedBy()).isEqualTo("admin-1");
        assertThat(owner.getRoles()).extracting(Role::getCode).contains(PredefinedRole.SELLER);
        verify(userRepository).save(owner);
    }

    @Test
    void reject_shouldOnlyAllowPendingShop() {
        Shop shop = buildShop(buildOwner(), ShopStatus.APPROVED);
        when(shopRepository.findWithOwnerById(1L)).thenReturn(Optional.of(shop));

        assertThatThrownBy(() -> adminShopService.reject(
                        1L, ShopModerationRequest.builder().reason("Bad data").build()))
                .isInstanceOf(AppException.class);
    }

    @Test
    void lockAndUnlock_shouldToggleApprovedAndLockedStatuses() {
        Shop shop = buildShop(buildOwner(), ShopStatus.APPROVED);
        ShopResponse locked =
                ShopResponse.builder().id(1L).status(ShopStatus.LOCKED).build();
        ShopResponse approved =
                ShopResponse.builder().id(1L).status(ShopStatus.APPROVED).build();

        when(shopRepository.findWithOwnerById(1L)).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shopMapper.toShopResponse(shop)).thenReturn(locked, approved);

        ShopResponse lockedResult = adminShopService.lock(
                1L, ShopModerationRequest.builder().reason("Policy issue").build());
        assertThat(lockedResult).isSameAs(locked);
        assertThat(shop.getStatus()).isEqualTo(ShopStatus.LOCKED);
        assertThat(shop.getLockedBy()).isEqualTo("admin-1");

        ShopResponse unlockedResult = adminShopService.unlock(1L);
        assertThat(unlockedResult).isSameAs(approved);
        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
        assertThat(shop.getLockedReason()).isNull();
    }

    private User buildOwner() {
        User user = new User();
        user.setId("user-1");
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setRoles(new HashSet<>());
        return user;
    }

    private Shop buildShop(User owner, ShopStatus status) {
        return Shop.builder()
                .id(1L)
                .owner(owner)
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
