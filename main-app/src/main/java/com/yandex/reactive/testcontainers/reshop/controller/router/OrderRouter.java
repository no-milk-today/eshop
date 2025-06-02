package com.yandex.reactive.testcontainers.reshop.controller.router;

import com.yandex.reactive.testcontainers.reshop.exception.PaymentException;
import com.yandex.reactive.testcontainers.reshop.exception.ResourceNotFoundException;
import com.yandex.reactive.testcontainers.reshop.controller.handler.OrderHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;


@Configuration
public class OrderRouter {

    @Bean
    public RouterFunction<ServerResponse> orderRoutes(OrderHandler handler) {
        return RouterFunctions.route()
                .path("/", builder -> builder
                        .POST("/buy", handler::buy)
                        .GET("/orders/{id}", handler::getOrder)
                        .GET("/orders", handler::orders)
                )
                .onError(ResourceNotFoundException.class, (ex, request) ->
                        ServerResponse.status(HttpStatus.NOT_FOUND)
                                .render("not-found", Map.of("errorMessage", ex.getMessage()))
                )
                .onError(PaymentException.class, (ex, request) ->
                        ServerResponse.status(HttpStatus.PAYMENT_REQUIRED)
                                .render("payment-error", Map.of("errorMessage", ex.getMessage()))
                )
                .onError(IllegalArgumentException.class, (ex, request) ->
                        ServerResponse.badRequest().build())
                .onError(Exception.class, (ex, request) ->
                        ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
                .build();
    }
}
