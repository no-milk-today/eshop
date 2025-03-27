package ru.practicum.spring.data.shop.dto;

public record Paging(
        int pageNumber,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious
) {
}