package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.client.api.PaymentApi;
import com.yandex.reactive.testcontainers.reshop.domain.BalanceResponse;
import com.yandex.reactive.testcontainers.reshop.domain.PaymentRequest;
import com.yandex.reactive.testcontainers.reshop.domain.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Disabled
public class PaymentClientServiceTest {

    private PaymentApi paymentApi;
    private PaymentClientService paymentClientService;
    private ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    @BeforeEach
    void setUp() {
        paymentApi = mock(PaymentApi.class);
        authorizedClientManager = request -> Mono.empty();
        paymentClientService = new PaymentClientService(paymentApi, authorizedClientManager);
    }

    @Test
    void testCheckBalanceTrue() {
        var userId = "user1";
        double amount = 100.0;
        var balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(150.0);
        when(paymentApi.getBalance(userId)).thenReturn(Mono.just(balanceResponse));

        var result = paymentClientService.checkBalance(userId, amount).block();
        assertThat(result).isTrue();
        verify(paymentApi, times(1)).getBalance(userId);
    }

    @Test
    void testCheckBalanceFalse() {
        var userId = "user1";
        double amount = 100.0;
        var balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(50.0);
        when(paymentApi.getBalance(userId)).thenReturn(Mono.just(balanceResponse));

        Boolean result = paymentClientService.checkBalance(userId, amount).block();
        assertThat(result).isFalse();
        verify(paymentApi, times(1)).getBalance(userId);
    }

    @Test
    void testMakePaymentSuccess() {
        var userId = "user2";
        double amount = 100.0;
        var paymentResponse = new PaymentResponse();
        paymentResponse.setStatus("SUCCESS");
        when(paymentApi.processPayment(any(PaymentRequest.class))).thenReturn(Mono.just(paymentResponse));

        var result = paymentClientService.makePayment(userId, amount).block();
        assertThat(result).isTrue();

        var captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentApi, times(1)).processPayment(captor.capture());
        var req = captor.getValue();
        assertThat(req.getUserId()).isEqualTo(userId);
        assertThat(req.getAmount()).isEqualTo(amount);
        assertThat(req.getCurrency()).isEqualTo("RUB");
    }

    @Test
    void testMakePaymentFailure() {
        var userId = "user2";
        double amount = 100.0;
        var paymentResponse = new PaymentResponse();
        paymentResponse.setStatus("FAILED");
        when(paymentApi.processPayment(any(PaymentRequest.class))).thenReturn(Mono.just(paymentResponse));

        var result = paymentClientService.makePayment(userId, amount).block();
        assertThat(result).isFalse();

        verify(paymentApi, times(1)).processPayment(any(PaymentRequest.class));
    }
}