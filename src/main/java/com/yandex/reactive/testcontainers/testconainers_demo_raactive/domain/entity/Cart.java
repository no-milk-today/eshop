package com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.annotation.Transient;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("carts")
public class Cart {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Transient
    private List<Product> products;

    @Column("total_price")
    private double totalPrice;
}
