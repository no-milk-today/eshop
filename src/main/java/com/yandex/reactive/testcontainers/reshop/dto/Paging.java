package com.yandex.reactive.testcontainers.reshop.dto;

public record Paging(
        int pageNumber,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious
) {
}