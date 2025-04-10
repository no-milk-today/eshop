package com.yandex.reactive.testcontainers.testconainers_demo_raactive.repository;

import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface UserRepository extends R2dbcRepository<User, Long> {
}
