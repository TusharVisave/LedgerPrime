package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Pagination envelope returned for list endpoints. */
@Getter
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
