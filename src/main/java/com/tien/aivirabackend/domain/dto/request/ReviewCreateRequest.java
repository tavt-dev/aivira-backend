package com.tien.aivirabackend.domain.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Customer review request for a completed purchased order item.")
public class ReviewCreateRequest {
    @Schema(description = "Rating from 1 to 5.", example = "5")
    @NotNull
    @Min(1)
    @Max(5)
    Integer rating;

    @Schema(example = "Excellent translation quality and sturdy paperback binding.")
    @Size(max = 2000)
    String comment;

    @Schema(description = "Optional review image metadata. Maximum 5 images.")
    @Valid
    @Size(max = 5)
    @Builder.Default
    List<ReviewImageRequest> images = new ArrayList<>();
}
