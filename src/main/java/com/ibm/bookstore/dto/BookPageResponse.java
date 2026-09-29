package com.ibm.bookstore.dto;

import java.util.List;

public record BookPageResponse(
        List<BookSummaryDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
}
