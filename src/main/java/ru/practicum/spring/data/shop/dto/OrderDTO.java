package ru.practicum.spring.data.shop.dto;

import java.time.LocalDateTime;
import java.util.List;
import ru.practicum.spring.data.shop.domain.entity.Product;

public record OrderDTO(
    Long id,
    Long userId,
    List<Product> products,
    LocalDateTime orderDate,
    String number,
    double totalSum
) {}