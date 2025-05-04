package com.yandex.reactive.testcontainers.reshop.config;

import com.yandex.reactive.testcontainers.reshop.client.ApiClient;
import com.yandex.reactive.testcontainers.reshop.client.api.PaymentApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentApiClientConfig {

    @Bean
    public PaymentApi paymentApi() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:8081");
        return new PaymentApi(apiClient);
    }
}