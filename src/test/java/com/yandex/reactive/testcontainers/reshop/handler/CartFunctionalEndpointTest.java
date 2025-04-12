package com.yandex.reactive.testcontainers.reshop.handler;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Cart;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.router.CartRouter;
import com.yandex.reactive.testcontainers.reshop.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({CartRouter.class, CartHandler.class})
@WebFluxTest
public class CartFunctionalEndpointTest {

    @MockitoBean
    private CartService cartService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testGetCart() {
        // prepare cart
        var cart = new Cart();
        cart.setId(1L);
        // assume, that cart contains 2 same products and one different
        var p1 = new Product(100L, "Product 100", 10.0, "Description 100", "img100.jpg", 0);
        var p2 = new Product(200L, "Product 200", 20.0, "Description 200", "img200.jpg", 0);
        cart.setProducts(Arrays.asList(p1, p1, p2));
        cart.setTotalPrice(10.0 * 2 + 20.0); // 40.0

        when(cartService.getCart()).thenReturn(Mono.just(cart));
        when(cartService.getProductCounts()).thenReturn(Mono.just(Map.of(100L, 2, 200L, 1)));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("Product 100"));
                    assertTrue(body.contains("40.0"));
                });
    }

    @Test
    void testModifyCartItem() {
        when(cartService.modifyItem(eq(100L), eq("plus")))
                .thenReturn(Mono.just(new Cart()));

        webTestClient.post()
                .uri("/cart/items/100?action=plus")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).modifyItem(100L, "plus");
    }
}
