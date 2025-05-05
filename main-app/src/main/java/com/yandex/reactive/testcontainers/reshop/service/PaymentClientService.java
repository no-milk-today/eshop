package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.client.api.PaymentApi;
import com.yandex.reactive.testcontainers.reshop.domain.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
public class PaymentClientService {
    private final PaymentApi paymentApi; // сгенерированный WebClient‑клиент

    public PaymentClientService(PaymentApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    public Mono<Boolean> checkBalance(String userId, double amount) {
        return paymentApi.getBalance(userId)
                .map(resp -> resp.getBalance() >= amount);
    }

    public Mono<Boolean> makePayment(String userId, double amount) {
        PaymentRequest req = new PaymentRequest()
                .userId(userId)
                .amount(amount)
                .currency("RUB");
        return paymentApi.processPayment(req)
                .map(resp -> resp.getStatus().equals("SUCCESS"));
    }

    public Mono<Boolean> healthCheck() {
        return paymentApi.getApiClient().getWebClient().get()
                .uri(paymentApi.getApiClient().getBasePath() + "/actuator/health")
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        log.debug("Health check OK");
                        return Mono.just(true);
                    } else {
                        log.warn("Health check failed with status: {}", response.statusCode());
                        return Mono.just(false);
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("Health check failed: " + e.getMessage());
                    return Mono.just(false); // false вместо проброса ошибки
                });
    }
}

