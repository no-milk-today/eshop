package com.yandex.reactive.testcontainers.testconainers_demo_raactive.repository;

import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.Cart;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CartRepository extends R2dbcRepository<Cart, Long> {
    Mono<Cart> findByUserId(Long userId);
}

