package com.yandex.reactive.testcontainers.reshop.domain.entity;

import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;

// id для уникальной идентификации записи в join‑таблице. Это упрощает работу с удалением конкретной записи.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("cart_products")
public class CartProduct {

    @Id
    private Long id;
    private Long cartId;
    private Long productId;
}