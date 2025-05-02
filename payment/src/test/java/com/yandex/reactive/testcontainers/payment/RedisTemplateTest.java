package com.yandex.reactive.testcontainers.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RedisTemplateTest extends AbstractTestContainerTest {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    void testTtlInRedis() throws InterruptedException {
        // Сохраняем запись в Redis с TTL = 1 секунда
        redisTemplate.opsForValue().set("weather:spb", "+15", 1, TimeUnit.SECONDS);

        // Проверяем наличие записи в Redis
        assertThat(redisTemplate.opsForValue().get("weather:spb"))
                .withFailMessage("Пока не прошёл TTL, запись должна быть доступна")
                .isNotNull()
                .isEqualTo("+15");

        // Ждём, пока TTL истечёт
        TimeUnit.SECONDS.sleep(2L);

        // Проверяем, что запись пропала
        assertThat(redisTemplate.opsForValue().get("weather:spb"))
                .withFailMessage("После истечения TTL запись должна пропасть")
                .isNull();
    }

}
