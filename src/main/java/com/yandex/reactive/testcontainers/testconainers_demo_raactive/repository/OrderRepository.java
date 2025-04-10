package com.yandex.reactive.testcontainers.testconainers_demo_raactive.repository;


import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.Order;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, Long> {
    Flux<Order> findByNumber(String number);
}
