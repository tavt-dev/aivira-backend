package com.tien.aivirabackend.service.dashboard;

import java.time.Instant;

import com.tien.aivirabackend.domain.dto.response.DashboardLowStockResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardOrdersResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardSalesResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardSummaryResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardTopBooksResponse;

public interface DashboardService {
    DashboardSummaryResponse getSummary(Instant fromDate, Instant toDate);

    DashboardSalesResponse getSales(Instant fromDate, Instant toDate);

    DashboardOrdersResponse getOrders(Instant fromDate, Instant toDate);

    DashboardTopBooksResponse getTopBooks(Instant fromDate, Instant toDate, int limit);

    DashboardLowStockResponse getLowStock(Integer threshold, int limit);
}
