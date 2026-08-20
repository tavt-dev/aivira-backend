package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tien.aivirabackend.exception.GlobalExceptionHandler;
import com.tien.aivirabackend.service.dashboard.DashboardService;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerContractTest {
    @Mock
    DashboardService dashboardService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(dashboardService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void endpoints_shouldRequireDashboardOrReportPermission() throws Exception {
        assertDashboardPermission(AdminDashboardController.class.getMethod("getSummary", Instant.class, Instant.class));
        assertDashboardPermission(AdminDashboardController.class.getMethod("getSales", Instant.class, Instant.class));
        assertDashboardPermission(AdminDashboardController.class.getMethod("getOrders", Instant.class, Instant.class));
        assertDashboardPermission(
                AdminDashboardController.class.getMethod("getTopBooks", Instant.class, Instant.class, int.class));
        assertDashboardPermission(AdminDashboardController.class.getMethod("getLowStock", Integer.class, int.class));
    }

    @Test
    void endpoints_shouldDelegateToDashboardService() throws Exception {
        mockMvc.perform(get("/admin/dashboard/summary").param("fromDate", "2026-01-01T00:00:00Z").param("toDate",
                "2026-01-31T23:59:59Z")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/dashboard/sales").param("fromDate", "2026-01-01T00:00:00Z").param("toDate",
                "2026-01-31T23:59:59Z")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/dashboard/orders").param("fromDate", "2026-01-01T00:00:00Z").param("toDate",
                "2026-01-31T23:59:59Z")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/dashboard/top-books").param("fromDate", "2026-01-01T00:00:00Z")
                .param("toDate", "2026-01-31T23:59:59Z").param("limit", "7")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/dashboard/low-stock").param("threshold", "3").param("limit", "4"))
                .andExpect(status().isOk());

        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        verify(dashboardService).getSummary(from, to);
        verify(dashboardService).getSales(from, to);
        verify(dashboardService).getOrders(from, to);
        verify(dashboardService).getTopBooks(from, to, 7);
        verify(dashboardService).getLowStock(3, 4);
    }

    private void assertDashboardPermission(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("DASHBOARD_READ_ADMIN");
        assertThat(preAuthorize.value()).contains("REPORT_READ_ALL");
    }
}
