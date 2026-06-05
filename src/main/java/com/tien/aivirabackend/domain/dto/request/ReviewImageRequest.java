package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Review image metadata. Upload handling is outside the review endpoint.")
public class ReviewImageRequest {
    @Schema(example = "https://res.cloudinary.com/aivira/image/upload/reviews/clean-code-1.jpg")
    @NotBlank
    @Size(max = 500)
    String imageUrl;

    @Schema(example = "reviews/clean-code-1")
    @NotBlank
    @Size(max = 255)
    String imagePublicId;

    @Schema(example = "0")
    @NotNull
    @PositiveOrZero
    Integer sortOrder;
}
