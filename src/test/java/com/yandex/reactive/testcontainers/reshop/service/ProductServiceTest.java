package com.yandex.reactive.testcontainers.reshop.service;

import org.junit.jupiter.api.Test;


import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.repository.ProductRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService underTest;

    @Test
    void testFindById() {
        var product = new Product();
        product.setId(100L);
        product.setName("Test product");
        product.setPrice(9.99);
        product.setDescription("Test description");
        product.setImgPath("https://example.com/image.jpg");

        when(productRepository.findById(100L)).thenReturn(Mono.just(product));

        var resultMono = underTest.findById(100L);
        var result = resultMono.block();

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test product");
    }

    @Test
    void testGetProductsWithoutSearch() {
        List<Product> products = List.of(
                new Product(1L, "Test product 1", 1.2, "testdescription1", "https://example.com/test1.jpg", 0),
                new Product(2L, "Test product 2", 2.5, "testdescription2", "https://example.com/test2.jpg", 0),
                new Product(3L, "Test product 3", 3.0, "testdescription3", "https://example.com/test4.jpg", 0),
                new Product(4L, "Test product 5", 4.0, "testdescription4", "https://example.com/test5.jpg", 0)
        );

        when(productRepository.findAll()).thenReturn(Flux.fromIterable(products));

        var result = underTest.getProducts("", "ALPHA", 1, 3).collectList().block();

        assertThat(result)
                .isNotNull()
                .hasSize(3)
                .extracting(Product::getName)
                .containsExactly("Test product 1", "Test product 2", "Test product 3");
    }

    @Test
    void testGetProductsSortedByPrice() {
        List<Product> products = List.of(
                new Product(1L, "Test product 1", 3.0, "testdescription1", "https://example.com/test1.jpg", 0),
                new Product(2L, "Test product 2", 1.2, "testdescription2", "https://example.com/test2.jpg", 0),
                new Product(3L, "Test product 3", 4.0, "testdescription3", "https://example.com/test3.jpg", 0),
                new Product(4L, "Test product 4", 2.5, "testdescription4", "https://example.com/test4.jpg", 0)
        );

        when(productRepository.findAll()).thenReturn(Flux.fromIterable(products));

        var result = underTest.getProducts("", "PRICE", 1, 4).collectList().block();

        assertThat(result)
                .isNotNull()
                .hasSize(4)
                .extracting(Product::getPrice)
                .containsExactly(1.2, 2.5, 3.0, 4.0);
    }

    @Test
    void testGroupProducts() {
        List<Product> products = List.of(
                new Product(1L, "A", 1.0, "desc", "img", 0),
                new Product(2L, "B", 2.0, "desc", "img", 0),
                new Product(3L, "C", 3.0, "desc", "img", 0),
                new Product(4L, "D", 4.0, "desc", "img", 0),
                new Product(5L, "E", 5.0, "desc", "img", 0)
        );
        Flux<Product> flux = Flux.fromIterable(products);

        // groupProducts собирает список продуктов в ряды по 3 штуки
        var grouped = underTest.groupProducts(flux).block();

        assertThat(grouped)
                .withFailMessage("Ожидалось 2 ряда продуктов, но результат пустой")
                .isNotEmpty()
                .withFailMessage("Ожидалось 2 ряда продуктов")
                .hasSize(2);

        assertThat(grouped.get(0))
                .withFailMessage("В первом ряду должно быть 3 продукта")
                .hasSize(3);

        assertThat(grouped.get(1))
                .withFailMessage("Во втором ряду должно быть 2 продукта")
                .hasSize(2);
    }
}
