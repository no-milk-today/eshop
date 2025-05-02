package com.yandex.reactive.testcontainers.reshop.repository;

import com.yandex.reactive.testcontainers.reshop.domain.entity.OrderProduct;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;


public interface OrderProductRepository extends ReactiveCrudRepository<OrderProduct, Long> {
    Flux<OrderProduct> findByOrderId(Long orderId);
    Flux<OrderProduct> findByOrderIdAndProductId(Long orderId, Long productId);
}

