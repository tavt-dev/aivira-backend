package com.tien.aivirabackend.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.dto.request.ApplyShopRequest;
import com.tien.aivirabackend.domain.dto.request.UpdateShopRequest;
import com.tien.aivirabackend.domain.dto.response.SellerDashboardResponse;
import com.tien.aivirabackend.domain.dto.response.ShopResponse;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.ShopMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.ShopErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.ShopRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.CloudinaryStorageService;
import com.tien.aivirabackend.service.CloudinaryUploadResult;
import com.tien.aivirabackend.service.CurrentUserService;
import com.tien.aivirabackend.service.FileValidatorService;
import com.tien.aivirabackend.service.ShopService;
import com.tien.aivirabackend.util.SlugUtils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "SHOP-SERVICE")
public class ShopServiceImpl implements ShopService {
    private static final int LOGO_WIDTH = 400;
    private static final int LOGO_HEIGHT = 400;

    ShopRepository shopRepository;
    UserRepository userRepository;
    ShopMapper shopMapper;
    FileValidatorService fileValidatorService;
    CloudinaryStorageService cloudinaryStorageService;
    CurrentUserService currentUserService;

    @Override
    @Transactional
    public ShopResponse apply(ApplyShopRequest request) {
        User currentUser = getCurrentUser();
        if (shopRepository.existsByOwnerId(currentUser.getId())) {
            throw new AppException(ShopErrorCode.SHOP_ALREADY_EXISTS);
        }

        Shop shop = Shop.builder()
                .owner(currentUser)
                .shopName(request.getShopName().trim())
                .slug(generateUniqueSlug(request.getShopName()))
                .description(trimToNull(request.getDescription()))
                .businessEmail(request.getBusinessEmail().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .legalName(request.getLegalName().trim())
                .taxCode(trimToNull(request.getTaxCode()))
                .pickupAddressLine(request.getPickupAddressLine().trim())
                .pickupWard(trimToNull(request.getPickupWard()))
                .pickupDistrict(trimToNull(request.getPickupDistrict()))
                .pickupCity(request.getPickupCity().trim())
                .status(ShopStatus.PENDING)
                .build();

        Shop savedShop = shopRepository.save(shop);
        log.info("User {} applied for shop {}", currentUser.getId(), savedShop.getId());
        return shopMapper.toShopResponse(savedShop);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopResponse getMyShop() {
        return shopMapper.toShopResponse(getCurrentUserShop());
    }

    @Override
    @Transactional
    public ShopResponse updateMyShop(UpdateShopRequest request) {
        Shop shop = getCurrentUserShop();
        validateUpdateAllowed(shop);

        if (StringUtils.hasText(request.getShopName())) {
            String newShopName = request.getShopName().trim();
            if (!newShopName.equals(shop.getShopName())) {
                shop.setShopName(newShopName);
                shop.setSlug(generateUniqueSlug(newShopName));
            }
        }
        if (request.getDescription() != null) {
            shop.setDescription(trimToNull(request.getDescription()));
        }
        if (StringUtils.hasText(request.getBusinessEmail())) {
            shop.setBusinessEmail(request.getBusinessEmail().trim());
        }
        if (StringUtils.hasText(request.getPhoneNumber())) {
            shop.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (StringUtils.hasText(request.getLegalName())) {
            shop.setLegalName(request.getLegalName().trim());
        }
        if (request.getTaxCode() != null) {
            shop.setTaxCode(trimToNull(request.getTaxCode()));
        }
        if (StringUtils.hasText(request.getPickupAddressLine())) {
            shop.setPickupAddressLine(request.getPickupAddressLine().trim());
        }
        if (request.getPickupWard() != null) {
            shop.setPickupWard(trimToNull(request.getPickupWard()));
        }
        if (request.getPickupDistrict() != null) {
            shop.setPickupDistrict(trimToNull(request.getPickupDistrict()));
        }
        if (StringUtils.hasText(request.getPickupCity())) {
            shop.setPickupCity(request.getPickupCity().trim());
        }

        Shop savedShop = shopRepository.save(shop);
        log.info("User {} updated shop {}", savedShop.getOwner().getId(), savedShop.getId());
        return shopMapper.toShopResponse(savedShop);
    }

    @Override
    @Transactional
    public ShopResponse resubmitMyShop() {
        Shop shop = getCurrentUserShop();
        if (shop.getStatus() != ShopStatus.REJECTED) {
            throw new AppException(ShopErrorCode.SHOP_INVALID_STATUS_TRANSITION);
        }

        shop.setStatus(ShopStatus.PENDING);
        shop.setRejectionReason(null);
        shop.setRejectedAt(null);
        shop.setRejectedBy(null);

        Shop savedShop = shopRepository.save(shop);
        log.info("User {} resubmitted shop {}", savedShop.getOwner().getId(), savedShop.getId());
        return shopMapper.toShopResponse(savedShop);
    }

    @Override
    @Transactional
    public ShopResponse updateMyShopLogo(MultipartFile logoFile) {
        Shop shop = getCurrentUserShop();
        validateUpdateAllowed(shop);

        fileValidatorService.validateFile(logoFile, MediaType.IMAGE);
        CloudinaryUploadResult uploadResult = cloudinaryStorageService.uploadImage(
                logoFile,
                "aivira/shops/" + shop.getId() + "/logo",
                "shop-logo-" + shop.getId(),
                LOGO_WIDTH,
                LOGO_HEIGHT);

        shop.setLogoUrl(uploadResult.secureUrl());
        shop.setLogoPublicId(uploadResult.publicId());

        Shop savedShop = shopRepository.save(shop);
        log.info("User {} updated shop {} logo", savedShop.getOwner().getId(), savedShop.getId());
        return shopMapper.toShopResponse(savedShop);
    }

    @Override
    @Transactional(readOnly = true)
    public SellerDashboardResponse getDashboard() {
        Shop shop = getCurrentUserShop();
        return SellerDashboardResponse.builder()
                .shopId(shop.getId())
                .shopName(shop.getShopName())
                .shopStatus(shop.getStatus())
                .totalOrders(0)
                .pendingOrders(0)
                .totalProducts(0)
                .lowStockProducts(0)
                .revenue(0)
                .build();
    }

    private Shop getCurrentUserShop() {
        return shopRepository
                .findWithOwnerByOwnerId(getCurrentUserId())
                .orElseThrow(() -> new AppException(ShopErrorCode.SHOP_NOT_FOUND));
    }

    private User getCurrentUser() {
        String userId = getCurrentUserId();
        return userRepository.findById(userId).orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));
    }

    private String getCurrentUserId() {
        return currentUserService.getCurrentUserId();
    }

    private void validateUpdateAllowed(Shop shop) {
        if (shop.getStatus() == ShopStatus.LOCKED || shop.getStatus() == ShopStatus.INACTIVE) {
            throw new AppException(ShopErrorCode.SHOP_UPDATE_NOT_ALLOWED);
        }
    }

    private String generateUniqueSlug(String shopName) {
        String baseSlug = SlugUtils.slugify(shopName, "shop");
        String candidate = baseSlug;
        int suffix = 2;
        while (shopRepository.existsBySlug(candidate)) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
