package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.client.api.PaymentApi;
import com.yandex.reactive.testcontainers.reshop.domain.BalanceResponse;
import com.yandex.reactive.testcontainers.reshop.domain.PaymentRequest;
import com.yandex.reactive.testcontainers.reshop.domain.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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

    // за генерацию OAuth2 access token отвечает Keycloak
    public Mono<Boolean> checkBalance(String userId, double amount) {
        OAuth2AuthorizeRequest authRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("storefront-machine")
                .principal("system")
                .build();

        return authorizedClientManager.authorize(authRequest)
                .flatMap(authorizedClient -> {
                    String token = authorizedClient.getAccessToken().getTokenValue();
                    return paymentApi.getApiClient().getWebClient().get()
                            .uri(paymentApi.getApiClient().getBasePath() + "/balances/" + userId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .exchangeToMono(response -> {
                                if (response.statusCode().is2xxSuccessful()) {
                                    return response.bodyToMono(BalanceResponse.class);
                                } else {
                                    return Mono.error(new RuntimeException("Balance check failed with status: " + response.statusCode()));
                                }
                            });
                })
                .map(balanceResp -> balanceResp.getBalance() >= amount)
                .onErrorResume(e -> {
                    log.error("Balance check failed: {}", e.getMessage());
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> makePayment(String userId, double amount) {
        PaymentRequest req = new PaymentRequest()
                .userId(userId)
                .amount(amount)
                .currency("RUB");

        OAuth2AuthorizeRequest authRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("storefront-machine")
                .principal("system") // doesnt matter, У client_credentials нет имени пользователя
                .build();

        return authorizedClientManager.authorize(authRequest)
                .flatMap(authorizedClient -> {
                    String token = authorizedClient.getAccessToken().getTokenValue();
                    return paymentApi.getApiClient().getWebClient().post()
                            .uri(paymentApi.getApiClient().getBasePath() + "/payments")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token) // Подставляем Bearer-токена в хедер Authorization
                            .bodyValue(req)
                            .exchangeToMono(response -> {
                                if (response.statusCode().is2xxSuccessful()) {
                                    return response.bodyToMono(PaymentResponse.class);
                                } else {
                                    return Mono.error(new RuntimeException("Payment failed with status: " + response.statusCode()));
                                }
                            });
                })
                .map(resp -> "SUCCESS".equals(resp.getStatus()))
                .onErrorResume(e -> {
                    log.error("Payment failed: {}", e.getMessage());
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> healthCheck() {
        OAuth2AuthorizeRequest authRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("storefront-machine")
                .principal("system")
                .build();

        return authorizedClientManager.authorize(authRequest)
                .flatMap(authorizedClient -> {
                    String accessToken = authorizedClient.getAccessToken().getTokenValue();
                    log.debug("Health check: Получен токен: {}", accessToken);
                    return paymentApi.getApiClient().getWebClient().get()
                            .uri(paymentApi.getApiClient().getBasePath() + "/actuator/health")
                            .headers(h -> h.setBearerAuth(accessToken))  // ещё один способ использования Bearer-токена
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

