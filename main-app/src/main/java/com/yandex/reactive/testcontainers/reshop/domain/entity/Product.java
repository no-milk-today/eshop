package com.yandex.reactive.testcontainers.reshop.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.annotation.Transient;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("products")
public class Product {

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("price")
    private double price;

    @Column("description")
    private String description;

    @Column("img_path")
    private String imgPath;

    @Transient
    private int count; // Количество товара в корзине (вычисляется динамически)
}
