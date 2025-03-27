package ru.practicum.spring.data.shop.domain.enums;

public enum ProductSort {
    NO,
    ALPHA,
    PRICE;

    public static ProductSort from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NO;
        }
        try {
            return ProductSort.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NO;
        }
    }
}