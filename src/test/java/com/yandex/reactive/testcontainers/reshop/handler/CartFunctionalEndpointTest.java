package com.yandex.reactive.testcontainers.reshop.handler;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Cart;
import com.yandex.reactive.testcontainers.reshop.domain.entity.CartProduct;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.repository.CartProductRepository;
import com.yandex.reactive.testcontainers.reshop.repository.ProductRepository;
import com.yandex.reactive.testcontainers.reshop.router.CartRouter;
import com.yandex.reactive.testcontainers.reshop.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.BodyInserters.fromFormData;

@Import({CartRouter.class, CartHandler.class})
@WebFluxTest
public class CartFunctionalEndpointTest {

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CartProductRepository cartProductRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testGetCartItems() {
        // Set up a Cart with id=1 and totalPrice=20.0
        var cart = new Cart();
        cart.setId(1L);
        cart.setTotalPrice(20.0);
        when(cartService.getCart()).thenReturn(Mono.just(cart));

        // Two entries for product 100 so that count is 2
        var cp1 = new CartProduct(1L, 1L, 100L);
        var cp2 = new CartProduct(2L, 1L, 100L);
        when(cartProductRepository.findByCartId(1L))
                .thenReturn(Flux.just(cp1, cp2));

        // Return a Product for id=100
        var product = new Product(100L, "Product A", 10.0, "Description A", "imgA.jpg", 0);
        when(productRepository.findById(100L)).thenReturn(Mono.just(product));

        // Perform GET request and verify response contains product info and total price
        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("Product A"));
                    assertTrue(body.contains("20.0"));
                });
    }

    @Test
    void testModifyCartItem() {
        when(cartService.modifyItem(eq(100L), eq("plus")))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/items/100")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData("action", "plus"))
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).modifyItem(100L, "plus");
    }
}
