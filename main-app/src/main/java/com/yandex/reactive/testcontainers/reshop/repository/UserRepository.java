package com.yandex.reactive.testcontainers.reshop.repository;

import com.yandex.reactive.testcontainers.reshop.domain.entity.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<User, Long> {
    Mono<User> findByUsername(String username);
    Mono<User> findByEmail(String email);
}
