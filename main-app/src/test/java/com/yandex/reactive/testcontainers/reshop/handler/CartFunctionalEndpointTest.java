package com.yandex.reactive.testcontainers.reshop.handler;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Cart;
import com.yandex.reactive.testcontainers.reshop.domain.entity.CartProduct;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.repository.CartProductRepository;
import com.yandex.reactive.testcontainers.reshop.repository.ProductRepository;
import com.yandex.reactive.testcontainers.reshop.router.CartRouter;
import com.yandex.reactive.testcontainers.reshop.service.CartService;
import com.yandex.reactive.testcontainers.reshop.service.PaymentClientService;
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

import static org.junit.jupiter.api.Assertions.*;
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

    @MockitoBean
    private PaymentClientService paymentClientService;

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Возвращает WebTestClient эмулирующий авторизованного OAuth2 юзера
     * и CSRF-токен для POST/PUT/DELETE
     */
    private WebTestClient clientWithLogin() {
        return webTestClient
                .mutateWith(
                    SecurityMockServerConfigurers.mockOAuth2Login()
                )
                .mutateWith(
                    SecurityMockServerConfigurers.csrf()
                );
    }

    @Test
    void testGetCartItemsEnoughMoney() {
        // Set up a Cart with id=1 and totalPrice=20.0 and userId=1
        var cart = new Cart();
        cart.setId(1L);
        cart.setTotalPrice(20.0);
        cart.setUserId(1L);
        when(cartService.getCart()).thenReturn(Mono.just(cart));

        // Two entries for product 100 so that count is 2
        var cp1 = new CartProduct(1L, 1L, 100L);
        var cp2 = new CartProduct(2L, 1L, 100L);
        when(cartProductRepository.findByCartId(1L))
                .thenReturn(Flux.just(cp1, cp2));

        // Return a Product for id=100
        var product = new Product(100L, "Product A", 10.0, "Description A", "imgA.jpg", 0);
        when(productRepository.findById(100L)).thenReturn(Mono.just(product));

        // Payment check returns true for both health and balance
        when(paymentClientService.healthCheck()).thenReturn(Mono.just(true));
        when(paymentClientService.checkBalance(eq(String.valueOf(cart.getUserId())), eq(cart.getTotalPrice())))
                .thenReturn(Mono.just(true));

        // Perform GET request and verify response contains product info and total price
        clientWithLogin().get()
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
                    // Button should NOT be disabled
                    assertFalse(body.contains("disabled"));
                    assertFalse(body.contains("Недостаточно средств на балансе"));
                    assertFalse(body.contains("Сервис платежей недоступен"));
                });
    }

    @Test
    void testGetCartItemsNotEnoughMoney() {
        // Set up a Cart with id=1 and totalPrice=150.0 and userId=1
        var cart = new Cart();
        cart.setId(1L);
        cart.setTotalPrice(150.0);
        cart.setUserId(1L);
        when(cartService.getCart()).thenReturn(Mono.just(cart));

        var cp = new CartProduct(1L, 1L, 200L);
        when(cartProductRepository.findByCartId(1L)).thenReturn(Flux.just(cp));

        var product = new Product(200L, "Expensive Product", 150.0, "Description B", "imgB.jpg", 0);
        when(productRepository.findById(200L)).thenReturn(Mono.just(product));

        // Payment check returns healthy but insufficient funds.
        when(paymentClientService.healthCheck()).thenReturn(Mono.just(true));
        when(paymentClientService.checkBalance(eq(String.valueOf(cart.getUserId())), eq(cart.getTotalPrice())))
                .thenReturn(Mono.just(false));

        // Perform GET request and verify the rendered HTML
        clientWithLogin().get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    // Button should be disabled
                    assertTrue(body.contains("Купить"));
                    assertTrue(body.contains("disabled"));
                    // Warning message must be
                    assertTrue(body.contains("Недостаточно средств на балансе"));
                    // Payment service is healthy so no service down message.
                    assertFalse(body.contains("Сервис платежей недоступен"));
                });
    }

    @Test
    void testGetCartItemsPaymentServiceDown() {
        var cart = new Cart();
        cart.setId(1L);
        cart.setTotalPrice(50.0);
        cart.setUserId(1L);
        when(cartService.getCart()).thenReturn(Mono.just(cart));

        var cp = new CartProduct(1L, 1L, 300L);
        when(cartProductRepository.findByCartId(1L)).thenReturn(Flux.just(cp));

        var product = new Product(300L, "Product C", 50.0, "Description C", "imgC.jpg", 0);
        when(productRepository.findById(300L)).thenReturn(Mono.just(product));

        when(paymentClientService.healthCheck()).thenReturn(Mono.just(false));
        when(paymentClientService.checkBalance(eq(String.valueOf(cart.getUserId())), eq(cart.getTotalPrice())))
                .thenReturn(Mono.just(true));

        clientWithLogin().get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("Купить"));
                    assertTrue(body.contains("disabled"));
                    assertTrue(body.contains("Сервис платежей недоступен, попробуйте позже"));
                    assertFalse(body.contains("Недостаточно средств на балансе"));
                });
    }

    @Test
    void testModifyCartItem() {
        when(cartService.modifyItem(eq(100L), eq("plus")))
                .thenReturn(Mono.empty());

        clientWithLogin().post()
                .uri("/cart/items/100")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData("action", "plus"))
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).modifyItem(100L, "plus");
    }

    @Test
    void testGetCartItemsPaymentServiceDownWithBalanceCheck() {
        var cart = new Cart();
        cart.setId(1L);
        cart.setTotalPrice(50.0);
        cart.setUserId(1L);
        when(cartService.getCart()).thenReturn(Mono.just(cart));

        var cp = new CartProduct(1L, 1L, 300L);
        when(cartProductRepository.findByCartId(1L)).thenReturn(Flux.just(cp));

        var product = new Product(300L, "Product C", 50.0, "Description C", "imgC.jpg", 0);
        when(productRepository.findById(300L)).thenReturn(Mono.just(product));

        // Simulate health check failure and balanceСheck true
        when(paymentClientService.healthCheck()).thenReturn(Mono.just(false));
        when(paymentClientService.checkBalance(eq(String.valueOf(cart.getUserId())), eq(cart.getTotalPrice())))
                .thenReturn(Mono.just(true));

        clientWithLogin().get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("Купить"));
                    assertTrue(body.contains("disabled"));
                    assertTrue(body.contains("Сервис платежей недоступен, попробуйте позже"));
                    assertFalse(body.contains("Недостаточно средств на балансе"));
                });
    }
}
