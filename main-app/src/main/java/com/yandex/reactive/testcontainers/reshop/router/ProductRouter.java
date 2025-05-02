package com.yandex.reactive.testcontainers.reshop.router;

import com.yandex.reactive.testcontainers.reshop.exception.ResourceNotFoundException;
import com.yandex.reactive.testcontainers.reshop.handler.ProductHandler;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ProductRouter {

    @Bean
    public RouterFunction<ServerResponse> productRoutes(ProductHandler handler) {
        return RouterFunctions.route()
                .path("/", builder -> builder
                        .GET("/", handler::index)
                        .GET("main/items", handler::mainItems)
                        .POST("main/items/{id}", handler::modifyMainItems)
                        .GET("items/{id}", handler::getSingleItem)
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