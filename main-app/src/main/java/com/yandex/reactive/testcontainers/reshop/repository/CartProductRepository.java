package com.yandex.reactive.testcontainers.reshop.repository;

import com.yandex.reactive.testcontainers.reshop.domain.entity.CartProduct;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface CartProductRepository extends ReactiveCrudRepository<CartProduct, Long> {
    Flux<CartProduct> findByCartId(Long cartId);
    Flux<CartProduct> findAllByCartIdAndProductId(Long cartId, Long productId);
    Mono<CartProduct> findFirstByCartIdAndProductId(Long cartId, Long productId);
}


