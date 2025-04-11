package com.yandex.reactive.testcontainers.reshop.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("users")
public class User {
    @Id
    private Long id;

    private String username;
    private String password;
    private String email;
}