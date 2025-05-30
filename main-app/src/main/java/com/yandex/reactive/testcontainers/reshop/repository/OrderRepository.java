package com.yandex.reactive.testcontainers.reshop.repository;


import com.yandex.reactive.testcontainers.reshop.domain.entity.Order;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, Long> {
    Flux<Order> findByUserId(Long userId);
    Flux<Order> findByNumber(String number);
}
