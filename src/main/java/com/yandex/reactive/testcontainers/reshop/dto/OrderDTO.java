package com.yandex.reactive.testcontainers.reshop.dto;


import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDTO(
    Long id,
    Long userId,
    List<Product> products,
    LocalDateTime orderDate,
    String number,
    double totalSum
) {}