package com.tien.aivirabackend.domain.dto;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    int currentPage;
    int totalPages;
    int pageSize;
    long totalElements;

    boolean hasNext;
    boolean hasPrevious;

    @Builder.Default
    List<T> data = Collections.emptyList();

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .currentPage(page.getNumber() + 1) // Spring 0-based -> 1-based
                .totalPages(page.getTotalPages())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .data(page.getContent() == null ? Collections.emptyList() : page.getContent())
                .build();
    }
}
