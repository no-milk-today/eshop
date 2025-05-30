package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.client.ApiClient;
import com.yandex.reactive.testcontainers.reshop.client.api.PaymentApi;
import com.yandex.reactive.testcontainers.reshop.domain.BalanceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.function.Function;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentClientServiceTest {

    @Mock
    private PaymentApi paymentApi;

    @Mock
    private ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    private PaymentClientService paymentClientService;

    @BeforeEach
    void setUp() {
        OAuth2AuthorizedClient authorizedClient = createMockOAuth2AuthorizedClient();
        when(authorizedClientManager.authorize(any())).thenReturn(Mono.just(authorizedClient));

        paymentClientService = new PaymentClientService(paymentApi, authorizedClientManager);
    }

    private OAuth2AuthorizedClient createMockOAuth2AuthorizedClient() {
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId("storefront-machine")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("https://test-keycloak/token")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "mock-access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        return new OAuth2AuthorizedClient(clientRegistration, "system", accessToken);
    }

    private void setupWebClientMocks() {
        ApiClient apiClient = mock(ApiClient.class);
        when(paymentApi.getApiClient()).thenReturn(apiClient);
        when(apiClient.getWebClient()).thenReturn(webClient);
        when(apiClient.getBasePath()).thenReturn("http://localhost:8081");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
    }

    @Test
    void testCheckBalance_Success_UserChecksOwnBalance() {
        setupWebClientMocks();

        var balanceResponse = new BalanceResponse();
        balanceResponse.setUserId("1");
        balanceResponse.setBalance(500.0);

        var successResponse = mock(ClientResponse.class);
        when(successResponse.statusCode()).thenReturn(HttpStatus.OK);
        when(successResponse.bodyToMono(BalanceResponse.class)).thenReturn(Mono.just(balanceResponse));

        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<BalanceResponse>> function = invocation.getArgument(0);
            return function.apply(successResponse);
        });
        var result = paymentClientService.checkBalance("1", 400.0).block();

        assertThat(result).isTrue();
        verify(paymentApi.getApiClient().getWebClient()).get();
    }

    @Test
    void testCheckBalance_Forbidden_UserTriesToAccessAnotherUserBalance() {
        setupWebClientMocks();

        var forbiddenResponse = mock(ClientResponse.class);
        when(forbiddenResponse.statusCode()).thenReturn(HttpStatus.FORBIDDEN);

        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<BalanceResponse>> function = invocation.getArgument(0);
            return function.apply(forbiddenResponse);
        });

        var result = paymentClientService.checkBalance("2", 100.0).block();

        assertThat(result).isFalse();
        verify(paymentApi.getApiClient().getWebClient()).get();
    }

    @Test
    void testCheckBalance_NetworkError_ReturnsFalse() {
        setupWebClientMocks();

        when(requestHeadersSpec.exchangeToMono(any(Function.class)))
                .thenReturn(Mono.error(new RuntimeException("Network error")));

        // Act & Assert
        StepVerifier.create(paymentClientService.checkBalance("1", 100.0))
                .expectNext(false)
                .verifyComplete();
    }
}

