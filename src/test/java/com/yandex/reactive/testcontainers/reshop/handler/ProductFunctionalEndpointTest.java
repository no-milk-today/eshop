package com.yandex.reactive.testcontainers.reshop.handler;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Cart;
import com.yandex.reactive.testcontainers.reshop.domain.entity.CartProduct;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.repository.CartProductRepository;
import com.yandex.reactive.testcontainers.reshop.router.ProductRouter;
import com.yandex.reactive.testcontainers.reshop.service.CartService;
import com.yandex.reactive.testcontainers.reshop.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.BodyInserters.fromFormData;

//todo: из коробки не видит роутер и хенждер, проресерчить вопрос
@Import({ProductRouter.class, ProductHandler.class})
@WebFluxTest
public class ProductFunctionalEndpointTest {

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CartProductRepository cartProductRepository;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testIndexRedirect() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main/items");
    }

    @Test
    void testModifyMainItemsRedirect() {
        // Simulate cartService.modifyItem returning an empty Mono
        when(cartService.modifyItem(eq(1L), eq("plus")))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/main/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                //.bodyValue("action=plus") // можно и так
                .body(fromFormData("action", "plus"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main/items");

        verify(cartService).modifyItem(1L, "plus");
    }

    @Test
    void testMainItems() {
        var product = new Product();
        product.setId(2L);
        product.setName("Default product");
        product.setPrice(5.55);
        product.setDescription("Default description");
        product.setImgPath("https://example.com/default.jpg");

        // без сорта
        List<List<Product>> groupedProducts = Collections.singletonList(Collections.singletonList(product));
        when(productService.getProducts(eq(""), eq("NO"), eq(1), eq(10)))
                .thenReturn(Flux.just(product));
        when(productService.groupProducts(ArgumentMatchers.<Flux<Product>>any()))
                .thenReturn(Mono.just(groupedProducts));

        // пустая корзина
        var cart = new Cart();
        cart.setId(10L);
        cart.setTotalPrice(0.0);
        when(cartService.getCart()).thenReturn(Mono.just(cart));
        when(cartProductRepository.findByCartId(eq(10L)))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/main/items")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<table")); // Проверяем, что страница содержит форму
                });
    }

    @Test
    void testMainItemsSortByPrice() {
        var product1 = new Product();
        product1.setId(3L);
        product1.setName("Cheap product");
        product1.setPrice(2.0);
        product1.setDescription("Cheap");
        product1.setImgPath("https://example.com/cheap.jpg");

        var product2 = new Product();
        product2.setId(4L);
        product2.setName("Expensive product");
        product2.setPrice(20.0);
        product2.setDescription("Expensive");
        product2.setImgPath("https://example.com/expensive.jpg");

        // сорт по price, ожидаем что первый будет дешевый
        Flux<Product> flux = Flux.just(product1, product2);
        List<List<Product>> groupedProducts = Collections.singletonList(List.of(product1, product2));
        when(productService.getProducts(eq(""), eq("PRICE"), eq(1), eq(10)))
                .thenReturn(flux);
        when(productService.groupProducts(ArgumentMatchers.<Flux<Product>>any()))
                .thenReturn(Mono.just(groupedProducts));

        // пусть для каждого продукта в Корзине count будет 1
        var cart = new Cart();
        cart.setId(20L);
        cart.setTotalPrice(22.0);
        when(cartService.getCart()).thenReturn(Mono.just(cart));

        var cpA = new CartProduct();
        cpA.setId(200L);
        cpA.setCartId(20L);
        cpA.setProductId(3L);
        var cpB = new CartProduct();
        cpB.setId(201L);
        cpB.setCartId(20L);
        cpB.setProductId(4L);
        when(cartProductRepository.findByCartId(eq(20L)))
                .thenReturn(Flux.just(cpA, cpB));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/main/items")
                        .queryParam("search", "")
                        .queryParam("sort", "PRICE")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    // Проверяем, что в теле отображается оба продукта, и порядок соответствует сортировке по цене
                    assertThat(body).contains("Cheap product");
                    assertThat(body).contains("Expensive product");
                    // Cheap product идёт раньше Expensive product
                    int indexCheap = body.indexOf("Cheap product");
                    int indexExpensive = body.indexOf("Expensive product");
                    assertThat(indexCheap).isLessThan(indexExpensive);
                });
    }

    @Test
    void testMainItemsViewAfterClickPlus() {
        var product = new Product();
        product.setId(1L);
        product.setName("Test product");
        product.setPrice(9.99);
        product.setDescription("Test description");
        product.setImgPath("https://example.com/image.jpg");

        // Предположим, что группировка возвращает одну строку с одним продуктом.
        List<List<Product>> groupedProducts = Collections.singletonList(Collections.singletonList(product));
        when(productService.getProducts(eq(""), eq("NO"), eq(1), eq(10)))
                .thenReturn(Flux.just(product));
        when(productService.groupProducts(ArgumentMatchers.<Flux<Product>>any()))
                .thenReturn(Mono.just(groupedProducts));

        // Симулируем корзину и записи join-таблицы, чтобы getProductCounts() вернуло count = 2 для продукта id=1
        var cart = new Cart();
        cart.setId(1L);
        cart.setTotalPrice(19.98);
        when(cartService.getCart()).thenReturn(Mono.just(cart));

        var cp1 = new CartProduct();
        cp1.setId(100L);
        cp1.setCartId(1L);
        cp1.setProductId(1L);
        var cp2 = new CartProduct();
        cp2.setId(101L);
        cp2.setCartId(1L);
        cp2.setProductId(1L);
        when(cartProductRepository.findByCartId(eq(1L)))
                .thenReturn(Flux.just(cp1, cp2));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/main/items")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertThat(body).contains("Test product");
                    // Проверяем, что в модели отображается количество 2 (например, можно найти "2" или текст, указывающий на count)
                    assertThat(body).contains("2");
                });
    }

    @Test
    void testGetSingleItemFound() {
        var product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Description");
        product.setPrice(123.45);
        product.setImgPath("images/test.jpg");

        when(productService.findById(eq(1L))).thenReturn(Mono.just(product));

        // return an empty cart
        var cart = new Cart();
        cart.setId(10L);
        cart.setTotalPrice(0.0);
        when(cartService.getCart()).thenReturn(Mono.just(cart));
        when(cartProductRepository.findByCartId(eq(10L))).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/items/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<form"));
                    assertTrue(body.contains("Test Product"));
                });
    }

}
