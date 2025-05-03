package com.yandex.reactive.testcontainers.payment.api.controller;


import com.yandex.reactive.testcontainers.payment.api.PaymentApiDelegate;
import com.yandex.reactive.testcontainers.payment.api.service.BalanceService;
import com.yandex.reactive.testcontainers.payment.model.BalanceResponse;
import com.yandex.reactive.testcontainers.payment.model.PaymentRequest;
import com.yandex.reactive.testcontainers.payment.model.PaymentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class PaymentApiDelegateImpl implements PaymentApiDelegate {

    private final BalanceService balanceService;

    public PaymentApiDelegateImpl(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(String userId, ServerWebExchange exchange) {
        return balanceService.getOrInitBalance(userId)
                .map(bal -> new BalanceResponse().userId(userId).balance(bal))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> processPayment(Mono<PaymentRequest> paymentRequestMono, ServerWebExchange exchange) {
        return paymentRequestMono.flatMap(req ->
                balanceService.handleIfPossible(req.getUserId(), req.getAmount())
                        .flatMap(success -> {
                            if (!success) return Mono.just(ResponseEntity.status(402).build());
                            var txId = UUID.randomUUID().toString();
                            var resp = new PaymentResponse().transactionId(txId).status("SUCCESS");
                            return Mono.just(ResponseEntity.ok(resp));
                        })
        );
    }
}
