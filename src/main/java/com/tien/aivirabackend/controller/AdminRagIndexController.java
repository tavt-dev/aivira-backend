package com.tien.aivirabackend.controller;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.response.*;
import com.tien.aivirabackend.service.ai.RagIndexService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/ai-advice/index")
@RequiredArgsConstructor
@Tag(name = "Admin RAG Index")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@authorizationService.hasPermission('PRODUCT_MANAGE_ALL')")
public class AdminRagIndexController {
    private final RagIndexService service;

    @PostMapping("/rebuild")
    public ResponseEntity<ApiResponse<RagIndexJobResponse>> rebuild() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("RAG reindex queued", service.startFullReindex()));
    }
    @PostMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<RagIndexJobResponse>> reindexProduct(@PathVariable Long productId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("Product reindex queued", service.reindexProduct(productId)));
    }
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<RagIndexJobResponse>> job(@PathVariable String jobId) {
        return ResponseEntity.ok(ApiResponse.success(service.getJob(jobId)));
    }
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<RagIndexStatusResponse>> status() {
        return ResponseEntity.ok(ApiResponse.success(service.status()));
    }
}
