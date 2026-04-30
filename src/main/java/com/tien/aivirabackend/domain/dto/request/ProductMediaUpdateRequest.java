package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductMediaUpdateRequest {
    @Size(max = 255)
    String altText;

    @Min(0)
    Integer sortOrder;

    Boolean primary;

    Boolean active;
}
