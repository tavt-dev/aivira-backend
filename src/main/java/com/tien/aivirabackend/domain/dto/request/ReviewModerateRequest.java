package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Admin review moderation request.")
public class ReviewModerateRequest {
    @Schema(description = "Approved reviews can be public if also visible and not deleted.", example = "true")
    @NotNull
    Boolean approved;

    @Schema(description = "Hidden reviews are excluded from public review lists.", example = "true")
    @NotNull
    Boolean visible;
}
