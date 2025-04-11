package com.yandex.reactive.testcontainers.reshop.repository;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Cart;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CartRepository extends R2dbcRepository<Cart, Long> {
    Mono<Cart> findByUserId(Long userId);
}

