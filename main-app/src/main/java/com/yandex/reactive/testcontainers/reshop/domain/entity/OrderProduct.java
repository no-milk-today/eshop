package com.yandex.reactive.testcontainers.reshop.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table("order_products")
@AllArgsConstructor
@NoArgsConstructor
public class OrderProduct {

    @Id
    private Long id;
    private Long orderId;
    private Long productId;
}
