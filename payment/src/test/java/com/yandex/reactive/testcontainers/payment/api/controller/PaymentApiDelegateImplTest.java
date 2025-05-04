package com.yandex.reactive.testcontainers.payment.api.controller;


import com.yandex.reactive.testcontainers.payment.api.service.BalanceService;
import com.yandex.reactive.testcontainers.payment.model.BalanceResponse;
import com.yandex.reactive.testcontainers.payment.model.PaymentRequest;
import com.yandex.reactive.testcontainers.payment.model.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentApiDelegateImplTest {

    private BalanceService balanceService;
    private PaymentApiDelegateImpl delegate;
    private ServerWebExchange exchange;

    @BeforeEach
    void setup() {
        balanceService = Mockito.mock(BalanceService.class);
        delegate = new PaymentApiDelegateImpl(balanceService);
        exchange = null; // пока не юзаем exchange
    }

    @Test
    void getBalance() {
        var userId = "user123";
        when(balanceService.getOrInitBalance(userId)).thenReturn(Mono.just(500.0));

        var response = delegate.getBalance(userId, exchange).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getUserId()).isEqualTo(userId);
        assertThat(body.getBalance()).isEqualTo(500.0);

        verify(balanceService, times(1)).getOrInitBalance(userId);
    }

    @Test
    void processPaymentNotEnoughMoney() {
        var req = new PaymentRequest()
                .userId("u1")
                .amount(1000.0)
                .currency("USD");

        when(balanceService.handleIfPossible(eq("u1"), eq(1000.0))).thenReturn(Mono.just(false));

        var response = delegate.processPayment(Mono.just(req), exchange).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCodeValue()).isEqualTo(402);

        verify(balanceService, times(1)).handleIfPossible("u1", 1000.0);
    }

    @Test
    void processPaymentSuccess() {
        var req = new PaymentRequest()
                .userId("u2")
                .amount(100.0)
                .currency("USD");
        when(balanceService.handleIfPossible("u2", 100.0)).thenReturn(Mono.just(true));

        var response = delegate.processPayment(Mono.just(req), exchange).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("SUCCESS");

        assertThat(UUID.fromString(body.getTransactionId())).isNotNull();

        verify(balanceService, times(1)).handleIfPossible("u2", 100.0);
    }
}
