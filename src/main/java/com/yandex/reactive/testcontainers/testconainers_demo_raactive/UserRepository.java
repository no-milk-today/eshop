package com.yandex.reactive.testcontainers.testconainers_demo_raactive;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface UserRepository extends R2dbcRepository<User, Long> {
}
