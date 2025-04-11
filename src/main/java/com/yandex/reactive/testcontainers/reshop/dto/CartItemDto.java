package com.yandex.reactive.testcontainers.reshop.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemDto {
    private Long id;
    private String name;
    private String description;
    private String imgPath;
    private double price;
    private int count;
}