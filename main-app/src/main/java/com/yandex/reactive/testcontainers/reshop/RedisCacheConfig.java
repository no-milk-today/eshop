package com.yandex.reactive.testcontainers.reshop;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer productsCacheCustomizer() {
        return builder -> builder
                .withCacheConfiguration(
                        "products",                    // Имя кеша
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))  // TTL 5 минут
                                .serializeValuesWith(             // Сериализация JSON
                                        RedisSerializationContext.SerializationPair.fromSerializer(
                                                new Jackson2JsonRedisSerializer<>(Product.class))
                                )
                );
    }

}

