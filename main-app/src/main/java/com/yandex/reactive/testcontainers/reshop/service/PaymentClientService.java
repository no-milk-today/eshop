package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.client.api.PaymentApi;
import com.yandex.reactive.testcontainers.reshop.domain.PaymentRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
                .currency("USD");
        return paymentApi.processPayment(req)
                .map(resp -> resp.getStatus().equals("SUCCESS"));
    }
}

