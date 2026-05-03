package com.tien.aivirabackend.service.product;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.ProductErrorCode;

@Component
public class ProductStatusPolicy {
    public void requireActiveVariation(Product product) {
        boolean hasActiveVariation = product.getProductVariations().stream()
                .anyMatch(variation -> Boolean.TRUE.equals(variation.getActive()));
        if (!hasActiveVariation) {
            throw new AppException(ProductErrorCode.PRODUCT_VARIATION_REQUIRED);
        }
    }

    public void moveEditableProductToDraft(Product product) {
        if (product.getStatus() == ProductStatus.ACTIVE
                || product.getStatus() == ProductStatus.PENDING_REVIEW
                || product.getStatus() == ProductStatus.REJECTED) {
            product.setStatus(ProductStatus.DRAFT);
            product.setSubmittedAt(null);
            product.setRejectionReason(null);
            product.setRejectedAt(null);
            product.setRejectedBy(null);
            product.setApprovedAt(null);
            product.setApprovedBy(null);
        }
        product.setActive(product.getStatus() != ProductStatus.INACTIVE);
    }

    public boolean isPubliclyVisible(Product product) {
        return product.getStatus() == ProductStatus.ACTIVE
                && Boolean.TRUE.equals(product.getActive())
                && product.getShop() != null
                && product.getShop().getStatus() == ShopStatus.APPROVED
                && product.getCategory() != null
                && Boolean.TRUE.equals(product.getCategory().getActive())
                && Boolean.TRUE.equals(product.getCategory().getVisible());
    }
}
