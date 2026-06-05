package com.tien.aivirabackend.domain.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewCreateRequest {
    @NotNull
    @Min(1)
    @Max(5)
    Integer rating;

    @Size(max = 2000)
    String comment;

    @Valid
    @Size(max = 5)
    @Builder.Default
    List<ReviewImageRequest> images = new ArrayList<>();
}
