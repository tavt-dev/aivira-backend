package com.tien.aivirabackend.service.impl;

import java.time.Instant;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ShopModerationRequest;
import com.tien.aivirabackend.domain.dto.response.ShopResponse;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.ShopMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.ShopErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.ShopRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.AdminShopService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "ADMIN-SHOP-SERVICE")
public class AdminShopServiceImpl implements AdminShopService {
    ShopRepository shopRepository;
    UserRepository userRepository;
    RoleRepository roleRepository;
    ShopMapper shopMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShopResponse> getShops(ShopStatus status, String keyword, int page, int size) {
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);
        var pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        var shopPage = shopRepository
                .findAll(buildShopSpecification(status, keyword), pageable)
                .map(shopMapper::toShopResponse);
        return PageResponse.from(shopPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopResponse getShop(Long shopId) {
        return shopMapper.toShopResponse(findShop(shopId));
    }

    @Override
    @Transactional
    public ShopResponse approve(Long shopId) {
        Shop shop = findShop(shopId);
        if (shop.getStatus() != ShopStatus.PENDING) {
            throw new AppException(ShopErrorCode.SHOP_INVALID_STATUS_TRANSITION);
        }

        shop.setStatus(ShopStatus.APPROVED);
        shop.setApprovedAt(Instant.now());
        shop.setApprovedBy(getCurrentUserId());
        shop.setRejectionReason(null);
        shop.setRejectedAt(null);
        shop.setRejectedBy(null);
        assignSellerRole(shop.getOwner());

        Shop savedShop = shopRepository.save(shop);
        log.info("Admin approved shop {}", savedShop.getId());
        return shopMapper.toShopResponse(savedShop);
    }

    @Override
    @Transactional
    public ShopResponse reject(Long shopId, ShopModerationRequest request) {
        Shop shop = findShop(shopId);
        if (shop.getStatus() != ShopStatus.PENDING) {
            throw new AppException(ShopErrorCode.SHOP_INVALID_STATUS_TRANSITION);
        }

        shop.setStatus(ShopStatus.REJECTED);
        shop.setRejectionReason(request.getReason().trim());
        shop.setRejectedAt(Instant.now());
        shop.setRejectedBy(getCurrentUserId());

        Shop savedShop = shopRepository.save(shop);
        log.info("Admin rejected shop {}", savedShop.getId());
        return shopMapper.toShopResponse(savedShop);
    }

    @Override
    @Transactional
    public ShopResponse lock(Long shopId, ShopModerationRequest request) {
        Shop shop = findShop(shopId);
        if (shop.getStatus() != ShopStatus.APPROVED) {
            throw new AppException(ShopErrorCode.SHOP_INVALID_STATUS_TRANSITION);
        }

        shop.setStatus(ShopStatus.LOCKED);
        shop.setLockedReason(request.getReason().trim());
        shop.setLockedAt(Instant.now());
        shop.setLockedBy(getCurrentUserId());

        Shop savedShop = shopRepository.save(shop);
        log.info("Admin locked shop {}", savedShop.getId());
        return shopMapper.toShopResponse(savedShop);
    }

    @Override
    @Transactional
    public ShopResponse unlock(Long shopId) {
        Shop shop = findShop(shopId);
        if (shop.getStatus() != ShopStatus.LOCKED) {
            throw new AppException(ShopErrorCode.SHOP_INVALID_STATUS_TRANSITION);
        }

        shop.setStatus(ShopStatus.APPROVED);
        shop.setLockedReason(null);
        shop.setLockedAt(null);
        shop.setLockedBy(null);

        Shop savedShop = shopRepository.save(shop);
        log.info("Admin unlocked shop {}", savedShop.getId());
        return shopMapper.toShopResponse(savedShop);
    }

    private Shop findShop(Long shopId) {
        return shopRepository
                .findWithOwnerById(shopId)
                .orElseThrow(() -> new AppException(ShopErrorCode.SHOP_NOT_FOUND));
    }

    private void assignSellerRole(User owner) {
        User managedOwner = userRepository
                .findById(owner.getId())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));
        Role sellerRole = roleRepository
                .findByCode(PredefinedRole.SELLER)
                .orElseThrow(() -> new AppException(UserErrorCode.ROLE_NOT_FOUND));

        boolean alreadySeller =
                managedOwner.getRoles().stream().anyMatch(role -> role.getCode() == PredefinedRole.SELLER);
        if (!alreadySeller) {
            managedOwner.getRoles().add(sellerRole);
            userRepository.save(managedOwner);
        }
    }

    private Specification<Shop> buildShopSpecification(ShopStatus status, String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("owner", jakarta.persistence.criteria.JoinType.LEFT);
                query.distinct(true);
            }
            Predicate predicate = criteriaBuilder.conjunction();

            if (status != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(keyword)) {
                String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("shopName")), likeKeyword);
                Predicate slugLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("slug")), likeKeyword);
                Predicate emailLike =
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("businessEmail")), likeKeyword);
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.or(nameLike, slugLike, emailLike));
            }

            return predicate;
        };
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        String userId = jwt.getClaimAsString("user_id");
        if (!StringUtils.hasText(userId)) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }
        return userId;
    }
}
