package com.yandex.reactive.testcontainers.reshop.handler;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Cart;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.router.ProductRouter;
import com.yandex.reactive.testcontainers.reshop.service.CartService;
import com.yandex.reactive.testcontainers.reshop.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//todo: из коробки не видит роутер и хенждер, проресерчить вопрос
@Import({ProductRouter.class, ProductHandler.class})
@WebFluxTest
public class ProductFunctionalEndpointTest {

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CartService cartService;

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
    void testMainItems() {
        var product1 = new Product();
        product1.setId(1L);
        product1.setName("Product1");
        var product2 = new Product();
        product2.setId(2L);
        product2.setName("Product2");
        var groupedProducts = List.of(List.of(product1, product2));

        when(productService.groupProducts(eq(""), eq("NO"), eq(1), eq(10)))
                .thenReturn(Mono.just(groupedProducts));

        // return empty Cart
        when(cartService.getProductCounts())
                .thenReturn(Mono.just(Collections.emptyMap()));

        // Create fake page with 2 products
        var productList = List.of(product1, product2);
        var productPage = new PageImpl<>(productList, PageRequest.of(0, 10), productList.size());
        when(productService.getProducts(eq(""), eq("NO"), eq(1), eq(10)))
                .thenReturn(Mono.just(productPage));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/main/items")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<table")); // Проверяем, что страница содержит форму
                });

        verify(productService).groupProducts("", "NO", 1, 10);
        verify(productService).getProducts("", "NO", 1, 10);
    }

    @Test
    void testModifyMainItems() {
        when(cartService.modifyItem(eq(1L), eq("plus")))
                .thenReturn(Mono.just(new Cart()));

        webTestClient.post()
                .uri("/main/items/1?action=plus")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/main/items");

        verify(cartService).modifyItem(1L, "plus");
    }

    @Test
    void testGetSingleItemFound() {
        var product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Description");
        product.setPrice(123.45);
        product.setImgPath("images/test.jpg");
        product.setCount(0);

        when(productService.findById(1L)).thenReturn(Mono.just(product));
        when(cartService.getProductCounts()).thenReturn(Mono.just(Collections.emptyMap()));

        webTestClient.get()
                .uri("/items/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<form"));
                });
    }

}
