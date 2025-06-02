package com.yandex.reactive.testcontainers.reshop.controller.router;

import com.yandex.reactive.testcontainers.reshop.exception.ResourceNotFoundException;
import com.yandex.reactive.testcontainers.reshop.controller.handler.CartHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

@Configuration
public class CartRouter {

    @Bean
    public RouterFunction<ServerResponse> cartRoutes(CartHandler handler) {
        return RouterFunctions.route()
                .path("/cart/items", builder -> builder
                        .GET("", handler::getCartItems)
                        .POST("/{id}", handler::modifyCartItem)
                )
                .onError(ResourceNotFoundException.class, (ex, request) ->
                        ServerResponse.status(HttpStatus.NOT_FOUND)
                                .render("not-found", Map.of("errorMessage", ex.getMessage()))
                )
                .onError(IllegalArgumentException.class, (ex, request) ->
                        ServerResponse.badRequest().build())
                .onError(Exception.class, (ex, request) ->
                        ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
                .build();
    }
}
