package com.yandex.redis.sampes;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.math.BigDecimal;

@RedisHash(value = "car", timeToLive = 10)
public record CarPrice(
        @Id
        String name,
        @Indexed
        BigDecimal temperature
) {}


