package com.tien.aivirabackend.domain.dto.response;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Admin dashboard top-selling books response.")
public class DashboardTopBooksResponse {
    @Schema(description = "Books sorted by sold quantity descending.")
    @Builder.Default
    List<TopBookResponse> books = new ArrayList<>();
}
