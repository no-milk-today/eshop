package com.yandex.reactive.testcontainers.payment.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redis;

    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        // Мокаем получение операций для работы со строковыми значениями в Redis
        when(redis.opsForValue()).thenReturn(valueOps);
        balanceService = new BalanceService(redis);
    }

    @Test
    void testGetOrInitBalanceWhenExists() {
        String userId = "user123";
        double existingBalance = 500.0;
        when(valueOps.get(userId)).thenReturn(Mono.just(String.valueOf(existingBalance)));

        Double balance = balanceService.getOrInitBalance(userId).block();

        assertThat(balance).isNotNull();
        assertThat(balance).isEqualTo(existingBalance);
    }

    @Test
    void testGetOrInitBalanceWhenNotExists() {
        String userId = "user456";
        when(valueOps.get(userId)).thenReturn(Mono.empty());
        when(valueOps.set(eq(userId), any(String.class))).thenReturn(Mono.just(Boolean.TRUE));

        Double balance = balanceService.getOrInitBalance(userId).block();

        assertThat(balance).isNotNull();
        assertThat(balance).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void testHandleIfPossible() {
        String userId = "user789";
        double initialBalance = 100.0;
        double amountToDeduct = 50.0;
        when(valueOps.get(userId)).thenReturn(Mono.just(String.valueOf(initialBalance)));
        when(valueOps.set(eq(userId), eq(String.valueOf(initialBalance - amountToDeduct))))
                .thenReturn(Mono.just(Boolean.TRUE));

        Boolean result = balanceService.handleIfPossible(userId, amountToDeduct).block();
        assertThat(result).isTrue();
    }

    @Test
    void testHandleIfPossibleNotEnough() {
        String userId = "user000";
        double initialBalance = 30.0;
        double amountToDeduct = 50.0;
        when(valueOps.get(userId)).thenReturn(Mono.just(String.valueOf(initialBalance)));

        Boolean result = balanceService.handleIfPossible(userId, amountToDeduct).block();
        assertThat(result).isFalse();
    }
}
