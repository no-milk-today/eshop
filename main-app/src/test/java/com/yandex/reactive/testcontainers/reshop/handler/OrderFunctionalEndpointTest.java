package com.yandex.reactive.testcontainers.reshop.handler;

import com.yandex.reactive.testcontainers.reshop.controller.handler.OrderHandler;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Order;
import com.yandex.reactive.testcontainers.reshop.domain.entity.User;
import com.yandex.reactive.testcontainers.reshop.exception.PaymentException;
import com.yandex.reactive.testcontainers.reshop.controller.router.OrderRouter;
import com.yandex.reactive.testcontainers.reshop.service.OrderProcessingService;
import com.yandex.reactive.testcontainers.reshop.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@Import({OrderRouter.class, OrderHandler.class})
@WebFluxTest
public class OrderFunctionalEndpointTest {

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderProcessingService orderProcessingService;

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient clientWithLogin() {
        return webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockOAuth2Login())
                .mutateWith(csrf());
    }

    @Test
    void testBuyOrder() {
        var order = new Order();
        order.setId(7L);
        order.setOrderDate(LocalDateTime.now());
        when(orderProcessingService.processOrder()).thenReturn(Mono.just(order));

        clientWithLogin().post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/7?newOrder=true");

        verify(orderProcessingService).processOrder();
    }

    @Test
    void testBuyOrderPaymentFailure() {
        when(orderProcessingService.processOrder())
                .thenReturn(Mono.error(new PaymentException("Payment failed due to insufficient funds")));

        clientWithLogin().post()
                .uri("/buy")
                .exchange()
                .expectStatus().isEqualTo(402)
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("Payment failed due to insufficient funds"));
                });
    }

    @Test
    void testGetOrderFound() {
        var order = new Order();
        order.setId(3L);
        order.setOrderDate(LocalDateTime.now());
        order.setNumber("#123456");

        var user = new User();
        user.setId(1L);
        order.setUserId(user.getId());
        order.setTotalSum(100.0);
        order.setProducts(Collections.emptyList());

        when(orderService.findByIdWithProducts(3L)).thenReturn(Mono.just(order));

        // GET /orders/3, без передачи newOrder (по дефолту false)
        clientWithLogin().get()
                .uri(uriBuilder -> uriBuilder.path("/orders/3").build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<table"));
                });
    }

    @Test
    void testGetOrderNotFound() {
        when(orderService.findByIdWithProducts(2L)).thenReturn(Mono.empty());

        clientWithLogin().get()
                .uri("/orders/2")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    if (body != null) {
                        assertTrue(body.contains("Not Found"));
                    }
                });
    }

    @Test
    void testOrdersForCurrentUser() {
        var order1 = new Order();
        order1.setId(100L);
        order1.setOrderDate(LocalDateTime.now());
        order1.setUserId(1L);
        order1.setProducts(Collections.emptyList());

        var order2 = new Order();
        order2.setId(101L);
        order2.setOrderDate(LocalDateTime.now());
        order2.setUserId(1L);
        order2.setProducts(Collections.emptyList());

        when(orderService.findOrdersForUsername(any())).thenReturn(Flux.just(order1, order2));
        when(orderService.findByIdWithProducts(100L)).thenReturn(Mono.just(order1));
        when(orderService.findByIdWithProducts(101L)).thenReturn(Mono.just(order2));

        clientWithLogin().get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<table"));
                });
    }
}
