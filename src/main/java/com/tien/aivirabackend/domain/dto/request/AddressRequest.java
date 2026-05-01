package com.tien.aivirabackend.domain.dto.request;

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
public class AddressRequest {
    @NotBlank
    @Size(max = 255)
    String recipientName;

    @NotBlank
    @Size(max = 30)
    String phoneNumber;

    @NotBlank
    @Size(max = 500)
    String addressLine;

    @Size(max = 255)
    String ward;

    @Size(max = 255)
    String district;

    @Size(max = 255)
    String city;

    Boolean defaultAddress;
}
