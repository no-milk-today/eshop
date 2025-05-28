package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.client.api.PaymentApi;
import com.yandex.reactive.testcontainers.reshop.domain.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
public class PaymentClientService {

    private final PaymentApi paymentApi; // сгенерированный WebClient‑клиент
    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    public PaymentClientService(PaymentApi paymentApi,
                                ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        this.paymentApi = paymentApi;
        this.authorizedClientManager = authorizedClientManager;
    }

    /**
     * Обновляем Bearer-токен из регистрации "storefront-machine" перед вызовом Payment API.
     */
    private Mono<Void> updateBearerToken() {
        OAuth2AuthorizeRequest authRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("storefront-machine")
                .principal("storefront-machine") // doesnt matter
                .build();

        return authorizedClientManager.authorize(authRequest)
                .doOnNext(authorizedClient -> {
                    String token = authorizedClient.getAccessToken().getTokenValue();
                    paymentApi.getApiClient().setBearerToken(token);
                })
                .then();
    }

    public Mono<Boolean> checkBalance(String userId, double amount) {
        return updateBearerToken()
                .then(paymentApi.getBalance(userId)
                        .map(resp -> resp.getBalance() >= amount));
    }

    public Mono<Boolean> makePayment(String userId, double amount) {
        PaymentRequest req = new PaymentRequest()
                .userId(userId)
                .amount(amount)
                .currency("RUB");
        return updateBearerToken()
                .then(paymentApi.processPayment(req)
                        .map(resp -> "SUCCESS".equals(resp.getStatus())));
    }

    public Mono<Boolean> healthCheck() {
        OAuth2AuthorizeRequest authRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("storefront-machine")
                .principal("storefront-machine")
                .build();

        return authorizedClientManager.authorize(authRequest)
                .flatMap(authorizedClient -> {
                    String token = authorizedClient.getAccessToken().getTokenValue();
                    log.debug("Health check: Получен токен: {}", token);
                    return paymentApi.getApiClient().getWebClient().get()
                            .uri(paymentApi.getApiClient().getBasePath() + "/actuator/health")
                            .header("Authorization", "Bearer " + token) // fixme: встраиваем токен прямо в запрос хелсчека
                            .exchangeToMono(response -> {
                                if (response.statusCode().is2xxSuccessful()) {
                                    log.debug("Health check OK");
                                    return Mono.just(true);
                                } else {
                                    log.warn("Health check failed with status: {}", response.statusCode());
                                    return Mono.just(false);
                                }
                            });
                })
                .onErrorResume(e -> {
                    log.error("Health check failed: {}", e.getMessage());
                    return Mono.just(false);
                });
    }
}

