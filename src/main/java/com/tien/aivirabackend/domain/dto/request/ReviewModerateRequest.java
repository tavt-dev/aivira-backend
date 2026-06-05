package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewModerateRequest {
    @NotNull
    Boolean approved;

    @NotNull
    Boolean visible;
}
