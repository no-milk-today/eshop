package com.yandex.reactive.testcontainers.reshop.repository;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductRepository extends R2dbcRepository<Product, Long> {
    Flux<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);

    @Query("SELECT * FROM products ORDER BY name ASC LIMIT :limit OFFSET :offset")
    Flux<Product> findAllProducts(int limit, int offset);

    @Query("SELECT COUNT(*) FROM products")
    Mono<Long> countProducts();

    @Query("SELECT * FROM products WHERE LOWER(name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "ORDER BY name ASC LIMIT :limit OFFSET :offset")
    Flux<Product> searchProducts(String search, int limit, int offset);

    @Query("SELECT COUNT(*) FROM products WHERE LOWER(name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Mono<Long> countProductsBySearch(String search);
}
