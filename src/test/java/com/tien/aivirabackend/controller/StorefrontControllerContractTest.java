package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tien.aivirabackend.domain.dto.response.StorefrontHomeResponse;
import com.tien.aivirabackend.exception.GlobalExceptionHandler;
import com.tien.aivirabackend.service.storefront.StorefrontService;

@ExtendWith(MockitoExtension.class)
class StorefrontControllerContractTest {
    @Mock
    StorefrontService storefrontService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StorefrontController(storefrontService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getHome_shouldBePublicAndReturnApiEnvelope() throws Exception {
        assertThat(StorefrontController.class.getMethod("getHome").getAnnotation(PreAuthorize.class))
                .isNull();

        org.mockito.Mockito.when(storefrontService.getHome())
                .thenReturn(StorefrontHomeResponse.builder().build());

        mockMvc.perform(get("/storefront/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.featuredBooks").isArray())
                .andExpect(jsonPath("$.data.newArrivals").isArray())
                .andExpect(jsonPath("$.data.bestsellingBooks").isArray())
                .andExpect(jsonPath("$.data.categoryHighlights").isArray());

        verify(storefrontService).getHome();
    }
}
