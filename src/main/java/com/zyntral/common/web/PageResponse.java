package com.zyntral.common.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable pagination envelope that decouples the API from Spring Data's {@code Page}
 * serialization (which is explicitly discouraged for public APIs).
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
