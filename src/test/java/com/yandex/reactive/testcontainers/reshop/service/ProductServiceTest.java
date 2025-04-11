package com.yandex.reactive.testcontainers.reshop.service;

import org.junit.jupiter.api.Test;


import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.repository.ProductRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    /**
     * Тест группировки товаров на строки по ITEMS_PER_ROW.
     * При вызове groupProducts происходит внутренний вызов getProducts,
     * который использует методы productRepository.findAllProducts(...) и countProducts().
     */
    @Test
    void testGroupProducts() {
        var product1 = new Product();
        product1.setName("Test product 1");
        product1.setPrice(9.99);
        product1.setDescription("This is a test product 1");
        product1.setImgPath("https://example.com/tshirt_2025_1.jpg");

        var product2 = new Product();
        product2.setName("Test product 2");
        product2.setPrice(9.99);
        product2.setDescription("This is a test product 2");
        product2.setImgPath("https://example.com/tshirt_2025_2.jpg");

        var product3 = new Product();
        product3.setName("Test product 3");
        product3.setPrice(9.99);
        product3.setDescription("This is a test product 3");
        product3.setImgPath("https://example.com/tshirt_2025_3.jpg");

        var product4 = new Product();
        product4.setName("Test product 4");
        product4.setPrice(9.99);
        product4.setDescription("This is a test product 4");
        product4.setImgPath("https://example.com/tshirt_2025_4.jpg");

        var product5 = new Product();
        product5.setName("Test product 5");
        product5.setPrice(9.99);
        product5.setDescription("This is a test product 5");
        product5.setImgPath("https://example.com/tshirt_2025_5.jpg");

        List<Product> products = List.of(product1, product2, product3, product4, product5);
        // Для страницы с pageNumber = 1 и pageSize = 5 => offset = 0, limit = 5
        when(productRepository.findAllProducts(5, 0))
                .thenReturn(Flux.fromIterable(products));
        when(productRepository.countProducts())
                .thenReturn(Mono.just((long) products.size()));

        Mono<List<List<Product>>> groupedMono = underTest.groupProducts("", "NO", 1, 5);
        List<List<Product>> grouped = groupedMono.block();

        // 5 продуктов разбиваются на 2 группы, первая содержит 3 продукта, вторая — 2
        assertThat(grouped).hasSize(2);
        assertThat(grouped.get(0)).hasSize(3);
        assertThat(grouped.get(1)).hasSize(2);
    }

    /**
     * Тест проверки метода getProducts без строки поиска и сортировки "NO".
     * тут сервис должен вызвать методы productRepository.findAllProducts(...) и countProducts().
     */
    @Test
    void testGetProductsWithoutSearchAndSort() {
        List<Product> productList = List.of(new Product(), new Product());
        // Для pageNumber = 1, pageSize = 10, offset = 0, limit = 10.
        when(productRepository.findAllProducts(10, 0))
                .thenReturn(Flux.fromIterable(productList));
        when(productRepository.countProducts())
                .thenReturn(Mono.just((long) productList.size()));

        Mono<Page<Product>> resultMono = underTest.getProducts("", "NO", 1, 10);
        Page<Product> result = resultMono.block();

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getPageable()).isEqualTo(PageRequest.of(0, 10, Sort.unsorted()));
    }

    /**
     * Тест проверки метода getProducts с сортировкой "ALPHA".
     * Тут сортировка формируется внутри сервиса, а для пустого поиска вызывается метод findAllProducts.
     */
    @Test
    void testGetProductsWithAlphaSort() {
        List<Product> productList = List.of(new Product(), new Product());
        when(productRepository.findAllProducts(10, 0))
                .thenReturn(Flux.fromIterable(productList));
        when(productRepository.countProducts())
                .thenReturn(Mono.just((long) productList.size()));

        Mono<Page<Product>> resultMono = underTest.getProducts("", "ALPHA", 1, 10);
        Page<Product> result = resultMono.block();

        assertThat(result.getContent()).hasSize(2);
        // Проверим, что Pageable сформирован с сортировкой по name
        assertThat(result.getPageable()).isEqualTo(PageRequest.of(0, 10, Sort.by("name").ascending()));
    }

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
}
