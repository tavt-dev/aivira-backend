package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressResponse {
    Long id;
    String recipientName;
    String phoneNumber;
    String addressLine;
    String ward;
    String district;
    String city;
    Boolean defaultAddress;
    Boolean active;
    Instant createdAt;
    Instant updatedAt;
}
