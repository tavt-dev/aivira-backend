package com.tien.aivirabackend.repository.projection;

import java.math.BigDecimal;

public interface TopBookProjection {
    Long getProductId();

    String getProductName();

    String getSku();

    String getThumbnailUrl();

    Long getQuantitySold();

    BigDecimal getRevenue();
}
