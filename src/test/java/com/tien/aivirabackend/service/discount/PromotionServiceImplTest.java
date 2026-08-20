package com.tien.aivirabackend.service.discount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.constant.PromotionScope;
import com.tien.aivirabackend.constant.PromotionType;
import com.tien.aivirabackend.domain.dto.request.PromotionCreateRequest;
import com.tien.aivirabackend.domain.dto.response.PromotionResponse;
import com.tien.aivirabackend.domain.entity.discount.Promotion;
import com.tien.aivirabackend.domain.mapper.DiscountMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.PromotionErrorCode;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.PromotionRepository;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {
    @Mock
    PromotionRepository promotionRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    CategoryRepository categoryRepository;

    PromotionServiceImpl promotionService;

    @BeforeEach
    void setUp() {
        promotionService = new PromotionServiceImpl(promotionRepository, productRepository, categoryRepository,
                new DiscountMapper());
    }

    @Test
    void createPromotion_shouldCreateProductScopePromotion() {
        when(promotionRepository.existsByPromotionName("Book Deal")).thenReturn(false);
        when(productRepository.existsById(10L)).thenReturn(true);
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PromotionResponse response = promotionService.createPromotion(createRequest(PromotionScope.PRODUCT, 10L));

        assertThat(response.getPromotionName()).isEqualTo("Book Deal");
        assertThat(response.getPromotionScope()).isEqualTo(PromotionScope.PRODUCT);
        assertThat(response.getTargetId()).isEqualTo(10L);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void createPromotion_whenDuplicateName_shouldThrow() {
        when(promotionRepository.existsByPromotionName("Book Deal")).thenReturn(true);

        assertThatThrownBy(() -> promotionService.createPromotion(createRequest(PromotionScope.PRODUCT, 10L)))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(PromotionErrorCode.PROMOTION_NAME_ALREADY_EXISTS));

        verify(promotionRepository, never()).save(any());
    }

    @Test
    void createPromotion_whenTargetMissing_shouldThrow() {
        when(promotionRepository.existsByPromotionName("Book Deal")).thenReturn(false);
        when(categoryRepository.existsById(20L)).thenReturn(false);

        assertThatThrownBy(() -> promotionService.createPromotion(createRequest(PromotionScope.CATEGORY, 20L)))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(PromotionErrorCode.PROMOTION_INVALID_TARGET));
    }

    @Test
    void deletePromotion_shouldDeactivateInsteadOfDeleting() {
        Promotion promotion = Promotion.builder().promotionName("Book Deal").active(true).build();
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion));

        promotionService.deletePromotion(1L);

        assertThat(promotion.getActive()).isFalse();
        verify(promotionRepository).save(promotion);
        verify(promotionRepository, never()).delete(any());
    }

    private PromotionCreateRequest createRequest(PromotionScope scope, Long targetId) {
        return PromotionCreateRequest.builder().promotionName(" Book Deal ").description("Deal for selected books")
                .promotionType(PromotionType.PERCENT).value(BigDecimal.TEN).maxDiscountAmount(BigDecimal.valueOf(30))
                .promotionScope(scope).targetId(targetId).startAt(LocalDateTime.now().minusDays(1))
                .endAt(LocalDateTime.now().plusDays(1)).build();
    }
}
