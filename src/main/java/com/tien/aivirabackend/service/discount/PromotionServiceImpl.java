package com.tien.aivirabackend.service.discount;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.PromotionScope;
import com.tien.aivirabackend.constant.PromotionType;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.PromotionCreateRequest;
import com.tien.aivirabackend.domain.dto.request.PromotionUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.PromotionResponse;
import com.tien.aivirabackend.domain.entity.discount.Promotion;
import com.tien.aivirabackend.domain.mapper.DiscountMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.PromotionErrorCode;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.PromotionRepository;
import com.tien.aivirabackend.util.PageRequestUtils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromotionServiceImpl implements PromotionService {
    PromotionRepository promotionRepository;
    ProductRepository productRepository;
    CategoryRepository categoryRepository;
    DiscountMapper discountMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> getPromotions(int page, int size) {
        return PageResponse.from(promotionRepository.findAll(PageRequestUtils.newestFirst(page, size))
                .map(discountMapper::toPromotionResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotion(Long promotionId) {
        return discountMapper.toPromotionResponse(findPromotion(promotionId));
    }

    @Override
    @Transactional
    public PromotionResponse createPromotion(PromotionCreateRequest request) {
        String name = trimRequired(request.getPromotionName());
        if (promotionRepository.existsByPromotionName(name)) {
            throw new AppException(PromotionErrorCode.PROMOTION_NAME_ALREADY_EXISTS);
        }
        validatePromotion(request.getPromotionType(), request.getValue(), request.getMaxDiscountAmount(),
                request.getPromotionScope(), request.getTargetId(), request.getStartAt(), request.getEndAt());
        Promotion promotion = Promotion.builder().promotionName(name)
                .description(trimRequired(request.getDescription())).promotionType(request.getPromotionType())
                .value(request.getValue()).maxDiscountAmount(request.getMaxDiscountAmount())
                .promotionScope(request.getPromotionScope()).targetId(request.getTargetId())
                .startAt(request.getStartAt()).endAt(request.getEndAt())
                .active(request.getActive() == null || Boolean.TRUE.equals(request.getActive())).build();
        return discountMapper.toPromotionResponse(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(Long promotionId, PromotionUpdateRequest request) {
        Promotion promotion = findPromotion(promotionId);
        if (request.getPromotionName() != null) {
            String name = trimRequired(request.getPromotionName());
            if (!name.equals(promotion.getPromotionName())
                    && promotionRepository.existsByPromotionNameAndIdNot(name, promotionId)) {
                throw new AppException(PromotionErrorCode.PROMOTION_NAME_ALREADY_EXISTS);
            }
            promotion.setPromotionName(name);
        }
        if (request.getDescription() != null) {
            promotion.setDescription(trimRequired(request.getDescription()));
        }
        if (request.getPromotionType() != null) {
            promotion.setPromotionType(request.getPromotionType());
        }
        if (request.getValue() != null) {
            promotion.setValue(request.getValue());
        }
        if (request.getMaxDiscountAmount() != null) {
            promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }
        if (request.getPromotionScope() != null) {
            promotion.setPromotionScope(request.getPromotionScope());
        }
        if (request.getTargetId() != null) {
            promotion.setTargetId(request.getTargetId());
        }
        if (request.getStartAt() != null) {
            promotion.setStartAt(request.getStartAt());
        }
        if (request.getEndAt() != null) {
            promotion.setEndAt(request.getEndAt());
        }
        if (request.getActive() != null) {
            promotion.setActive(request.getActive());
        }
        validatePromotion(promotion.getPromotionType(), promotion.getValue(), promotion.getMaxDiscountAmount(),
                promotion.getPromotionScope(), promotion.getTargetId(), promotion.getStartAt(), promotion.getEndAt());
        return discountMapper.toPromotionResponse(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public void deletePromotion(Long promotionId) {
        Promotion promotion = findPromotion(promotionId);
        promotion.setActive(false);
        promotionRepository.save(promotion);
    }

    private Promotion findPromotion(Long promotionId) {
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(PromotionErrorCode.PROMOTION_NOT_FOUND));
    }

    private void validatePromotion(PromotionType type, BigDecimal value, BigDecimal maxDiscountAmount,
            PromotionScope scope, Long targetId, java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
        if (type == null || value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(PromotionErrorCode.DISCOUNT_INVALID_VALUE);
        }
        if (type == PromotionType.PERCENT && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(PromotionErrorCode.DISCOUNT_INVALID_VALUE);
        }
        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(PromotionErrorCode.DISCOUNT_INVALID_VALUE);
        }
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new AppException(PromotionErrorCode.PROMOTION_INVALID_DATE_RANGE);
        }
        if (scope == null || targetId == null || targetId <= 0) {
            throw new AppException(PromotionErrorCode.PROMOTION_INVALID_TARGET);
        }
        boolean targetExists = scope == PromotionScope.PRODUCT ? productRepository.existsById(targetId)
                : categoryRepository.existsById(targetId);
        if (!targetExists) {
            throw new AppException(PromotionErrorCode.PROMOTION_INVALID_TARGET);
        }
    }

    private String trimRequired(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AppException(PromotionErrorCode.PROMOTION_INVALID_TARGET);
        }
        return value.trim();
    }
}
