package com.yandex.reactive.testcontainers.reshop.handler;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Order;
import com.yandex.reactive.testcontainers.reshop.domain.entity.User;
import com.yandex.reactive.testcontainers.reshop.router.OrderRouter;
import com.yandex.reactive.testcontainers.reshop.service.OrderProcessingService;
import com.yandex.reactive.testcontainers.reshop.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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

@Import({OrderRouter.class, OrderHandler.class})
@WebFluxTest
public class OrderFunctionalEndpointTest {

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderProcessingService orderProcessingService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testBuyOrder() {
        var order = new Order();
        order.setId(7L);
        order.setOrderDate(LocalDateTime.now());
        when(orderProcessingService.processOrder()).thenReturn(Mono.just(order));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/7?newOrder=true");

        verify(orderProcessingService).processOrder();
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

        when(orderService.findById(3L)).thenReturn(Mono.just(order));

        // GET /orders/3, без передачи newOrder (по дефолту false)
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/orders/3")
                        .build())
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
        when(orderService.findById(2L)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/orders/2")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("Not Found"));
                });
    }

    @Test
    void testOrdersListWithoutSortParam() {
        var order1 = new Order();
        order1.setId(10L);
        order1.setNumber("#10");
        order1.setOrderDate(LocalDateTime.now());
        order1.setProducts(Collections.emptyList());
        var order2 = new Order();
        order2.setId(11L);
        order2.setNumber("#11");
        order2.setOrderDate(LocalDateTime.now());
        order2.setProducts(Collections.emptyList());

        // Если sortBy нету, вызывается findAll()
        when(orderService.findAll()).thenReturn(Flux.just(order1, order2));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<table"));
                });

        verify(orderService).findAll();
    }

    @Test
    void testFindAllSortedOrders() {
        var order1 = new Order();
        order1.setId(1L);
        order1.setNumber("#100");
        order1.setOrderDate(LocalDateTime.now());
        order1.setProducts(Collections.emptyList());
        var order2 = new Order();
        order2.setId(2L);
        order2.setNumber("#101");
        order2.setOrderDate(LocalDateTime.now());
        order2.setProducts(Collections.emptyList());

        // sortBy есть, вызывается findAllSorted
        when(orderService.findAllSorted(any(Sort.class))).thenReturn(Flux.just(order1, order2));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/orders")
                        .queryParam("sortBy", "number")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<table"));
                });

        verify(orderService).findAllSorted(any(Sort.class));
    }
}
