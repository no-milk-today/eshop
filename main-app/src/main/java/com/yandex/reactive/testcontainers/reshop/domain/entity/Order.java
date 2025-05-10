package com.yandex.reactive.testcontainers.reshop.domain.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.annotation.Transient;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Transient
    private List<Product> products;

    @Column("order_date")
    private LocalDateTime orderDate;

    @Column("number")
    private String number;

    @Column("total_sum")
    private double totalSum;
}
