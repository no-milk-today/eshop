package com.yandex.reactive.testcontainers.reshop.router;

import com.yandex.reactive.testcontainers.reshop.handler.ProductHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class ProductRouter {

    @Bean
    public RouterFunction<ServerResponse> productRoutes(ProductHandler handler) {
        return route(GET("/"), handler::index)
                .andRoute(GET("/main/items"), handler::mainItems)
                .andRoute(POST("/main/items/{id}"), handler::modifyMainItems)
                .andRoute(GET("/items/{id}"), handler::getSingleItem);
    }

}

