package com.finpulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic wrapper for paginated API responses.
 *
 * Converts Spring's Page<T> into a frontend-friendly JSON structure:
 * {
 *   "content": [...],           // the actual items
 *   "currentPage": 0,           // zero-indexed page number
 *   "totalPages": 5,            // total number of pages
 *   "totalElements": 47,        // total matching records across all pages
 *   "pageSize": 10,             // items per page
 *   "hasNext": true,            // is there a next page?
 *   "hasPrevious": false        // is there a previous page?
 * }
 *
 * Your frontend can use these values directly:
 * - totalPages → render page buttons
 * - hasNext/hasPrevious → enable/disable next/prev buttons
 * - totalElements → show "Showing 1-10 of 47 results"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> content;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    /**
     * Factory method: converts a Spring Page<T> into our PagedResponse<T>.
     * Usage:
     *   Page<TransactionResponse> page = ...;
     *   PagedResponse<TransactionResponse> response = PagedResponse.from(page);
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}