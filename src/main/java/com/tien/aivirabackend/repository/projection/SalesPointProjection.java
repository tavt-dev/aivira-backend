package com.tien.aivirabackend.repository.projection;

import java.math.BigDecimal;

public interface SalesPointProjection {
    Object getSalesDate();

    BigDecimal getRevenue();
}
