package com.example.petlife.dto.common;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long total
) {
    public int totalPages() {
        return size <= 0 ? 1 : (int) Math.ceil((double) total / size);
    }

    public boolean hasPrev() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages();
    }
}
