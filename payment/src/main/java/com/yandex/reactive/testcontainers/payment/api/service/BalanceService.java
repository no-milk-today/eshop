package com.yandex.reactive.testcontainers.payment.api.service;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BalanceService {
    private final ReactiveStringRedisTemplate redis;

    public BalanceService(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Возвращает текущий баланс, если он есть в Redis,
     * или инициализирует случайным значением и сохраняет.
     */
    public Mono<Double> getOrInitBalance(String userId) {
        return redis.opsForValue()
                .get(userId)
                .flatMap(val -> Mono.just(Double.valueOf(val)))
                .switchIfEmpty(Mono.defer(() -> {
                    double init = Math.random() * 1000;
                    return redis.opsForValue()
                            .set(userId, String.valueOf(init))
                            .thenReturn(init);
                }));
    }

    /**
     * Пытается списать amount с баланса пользователя.
     * @return Mono<true> если хватило средств и баланс списан, иначе Mono<false>.
     */
    public Mono<Boolean> handleIfPossible(String userId, double amount) {
        return getOrInitBalance(userId)
                .flatMap(bal -> {
                    if (bal >= amount) {
                        double newBal = bal - amount;
                        return redis.opsForValue()
                                .set(userId, String.valueOf(newBal))
                                .thenReturn(true);
                    } else {
                        return Mono.just(false);
                    }
                });
    }
}
