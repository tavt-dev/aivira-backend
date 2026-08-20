package com.tien.aivirabackend.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.request.AddressRequest;
import com.tien.aivirabackend.domain.dto.response.AddressResponse;
import com.tien.aivirabackend.service.commerce.AddressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/users/me/addresses")
@Tag(name = "Address")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressController {
    AddressService addressService;

    @GetMapping
    @Operation(summary = "List my addresses")
    @PreAuthorize("@authorizationService.hasPermission('ADDRESS_READ_SELF')")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses() {
        return ResponseEntity.ok(ApiResponse.success("Get addresses successful", addressService.getMyAddresses()));
    }

    @PostMapping
    @Operation(summary = "Create my address")
    @PreAuthorize("@authorizationService.hasPermission('ADDRESS_CREATE_SELF')")
    public ResponseEntity<ApiResponse<AddressResponse>> create(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Create address successful", addressService.create(request)));
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "Update my address")
    @PreAuthorize("@authorizationService.hasPermission('ADDRESS_UPDATE_SELF')")
    public ResponseEntity<ApiResponse<AddressResponse>> update(@PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Update address successful", addressService.update(addressId, request)));
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete my address")
    @PreAuthorize("@authorizationService.hasPermission('ADDRESS_DELETE_SELF')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long addressId) {
        addressService.delete(addressId);
        return ResponseEntity.ok(ApiResponse.success("Delete address successful", null));
    }

    @PutMapping("/{addressId}/default")
    @Operation(summary = "Set default address")
    @PreAuthorize("@authorizationService.hasPermission('ADDRESS_SET_DEFAULT_SELF')")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(@PathVariable Long addressId) {
        return ResponseEntity
                .ok(ApiResponse.success("Set default address successful", addressService.setDefault(addressId)));
    }
}
