package com.tien.aivirabackend.domain.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.domain.dto.response.ProductMediaResponse;
import com.tien.aivirabackend.domain.dto.response.ProductResponse;
import com.tien.aivirabackend.domain.dto.response.ProductVariationResponse;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductMedia;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;

@Component
public class ProductMapper {
    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        Category category = product.getCategory();
        List<ProductVariationResponse> variations = product.getProductVariations().stream()
                .sorted(Comparator.comparing(ProductVariation::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toVariationResponse)
                .toList();
        List<ProductMediaResponse> media = product.getProductMedia().stream()
                .sorted(Comparator.comparing(ProductMedia::getSortOrder).thenComparing(ProductMedia::getId))
                .map(this::toMediaResponse)
                .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .categoryId(category == null ? null : category.getId())
                .categoryName(category == null ? null : category.getCategoryName())
                .categorySlug(category == null ? null : category.getSlug())
                .sku(product.getSku())
                .productName(product.getProductName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .brand(product.getBrand())
                .material(product.getMaterial())
                .bookAuthor(product.getBookAuthor())
                .isbn(product.getIsbn())
                .publisher(product.getPublisher())
                .publicationYear(product.getPublicationYear())
                .bookLanguage(product.getBookLanguage())
                .pageCount(product.getPageCount())
                .bookFormat(product.getBookFormat())
                .dimensions(product.getDimensions())
                .thumbnailUrl(product.getThumbnailUrl())
                .thumbnailPublicId(product.getThumbnailPublicId())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discountPercentage(product.getDiscountPercentage())
                .weight(product.getWeight())
                .stockQuantity(product.getStockQuantity())
                .soldCount(product.getSoldCount())
                .averageRating(product.getAverageRating())
                .active(product.getActive())
                .featured(product.getFeatured())
                .status(product.getStatus())
                .rejectionReason(product.getRejectionReason())
                .submittedAt(product.getSubmittedAt())
                .approvedBy(product.getApprovedBy())
                .approvedAt(product.getApprovedAt())
                .rejectedBy(product.getRejectedBy())
                .rejectedAt(product.getRejectedAt())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .variations(variations)
                .media(media)
                .build();
    }

    public ProductVariationResponse toVariationResponse(ProductVariation variation) {
        if (variation == null) {
            return null;
        }
        return ProductVariationResponse.builder()
                .id(variation.getId())
                .sku(variation.getSku())
                .color(variation.getColor())
                .size(variation.getSize())
                .additionalPrice(variation.getAdditionalPrice())
                .stockQuantity(variation.getStockQuantity())
                .imageUrl(variation.getImageUrl())
                .imagePublicId(variation.getImagePublicId())
                .active(variation.getActive())
                .createdAt(variation.getCreatedAt())
                .updatedAt(variation.getUpdatedAt())
                .build();
    }

    public ProductMediaResponse toMediaResponse(ProductMedia media) {
        if (media == null) {
            return null;
        }
        return ProductMediaResponse.builder()
                .id(media.getId())
                .mediaUrl(media.getMediaUrl())
                .mediaPublicId(media.getMediaPublicId())
                .mediaType(media.getMediaType())
                .altText(media.getAltText())
                .sortOrder(media.getSortOrder())
                .primary(media.getPrimary())
                .active(media.getActive())
                .createdAt(media.getCreatedAt())
                .updatedAt(media.getUpdatedAt())
                .build();
    }
}
