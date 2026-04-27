package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplyShopRequest {
    @NotBlank
    @Size(min = 3, max = 150)
    String shopName;

    @Size(max = 1000)
    String description;

    @NotBlank
    @Email
    @Size(max = 120)
    String businessEmail;

    @NotBlank
    @Size(max = 20)
    String phoneNumber;

    @NotBlank
    @Size(max = 150)
    String legalName;

    @Size(max = 50)
    String taxCode;

    @NotBlank
    @Size(max = 500)
    String pickupAddressLine;

    @Size(max = 120)
    String pickupWard;

    @Size(max = 120)
    String pickupDistrict;

    @NotBlank
    @Size(max = 120)
    String pickupCity;
}
