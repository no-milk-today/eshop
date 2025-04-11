package com.yandex.reactive.testcontainers.reshop.repository;

import com.yandex.reactive.testcontainers.reshop.domain.entity.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface UserRepository extends R2dbcRepository<User, Long> {
}
