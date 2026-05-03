package com.tien.aivirabackend.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageRequestUtils {
    public static final int MAX_PAGE_SIZE = 100;

    private PageRequestUtils() {}

    public static PageRequest of(int page, int size, Sort sort) {
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageIndex, pageSize, sort);
    }

    public static PageRequest newestFirst(int page, int size) {
        return of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
