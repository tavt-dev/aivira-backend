package com.tien.aivirabackend.domain.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardTopBooksResponse {
    @Builder.Default
    List<TopBookResponse> books = new ArrayList<>();
}
