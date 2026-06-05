package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewImageRequest {
    @NotBlank
    @Size(max = 500)
    String imageUrl;

    @NotBlank
    @Size(max = 255)
    String imagePublicId;

    @NotNull
    @PositiveOrZero
    Integer sortOrder;
}
