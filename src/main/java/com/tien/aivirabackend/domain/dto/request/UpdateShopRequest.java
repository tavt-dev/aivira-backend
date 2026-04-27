package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateShopRequest {
    @Size(min = 3, max = 150)
    String shopName;

    @Size(max = 1000)
    String description;

    @Email
    @Size(max = 120)
    String businessEmail;

    @Size(max = 20)
    String phoneNumber;

    @Size(max = 150)
    String legalName;

    @Size(max = 50)
    String taxCode;

    @Size(max = 500)
    String pickupAddressLine;

    @Size(max = 120)
    String pickupWard;

    @Size(max = 120)
    String pickupDistrict;

    @Size(max = 120)
    String pickupCity;
}
