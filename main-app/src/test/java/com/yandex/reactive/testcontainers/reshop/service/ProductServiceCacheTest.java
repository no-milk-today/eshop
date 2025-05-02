package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.AbstractTestContainerTest;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class ProductServiceCacheTest extends AbstractTestContainerTest {

    @Autowired
    private ProductService underTest;

    @MockitoBean
    private ProductRepository productRepository;

    @Test
    void testFindByIdCache() {
        var product = new Product();
        product.setId(100L);
        product.setName("CachedProduct");
        product.setPrice(9.99);
        product.setDescription("Test description");
        product.setImgPath("https://example.com/image.jpg");

        // Fetching the product from the database
        when(productRepository.findById(100L)).thenReturn(Mono.just(product));
        var fromDB = underTest.findById(100L).block();
        assertThat(fromDB).isNotNull();
        assertThat(fromDB.getName()).isEqualTo("CachedProduct");
        verify(productRepository, times(1)).findById(100L);

        // Fetching the product from the Redis cache
        when(productRepository.findById(100L)).thenReturn(Mono.empty());
        var fromCache = underTest.findById(100L).block();
        assertThat(fromCache).isNotNull();
        assertThat(fromCache.getName()).isEqualTo("CachedProduct");
        verify(productRepository, times(1)).findById(100L);
    }

    @Test
    void testGetProductsCache() {
        List<Product> products = List.of(
                new Product(1L, "CachedProduct1", 1.0, "desc1", "https://example.com/image1.jpg", 0),
                new Product(2L, "CachedProduct2", 2.0, "desc2", "https://example.com/image2.jpg", 0),
                new Product(3L, "CachedProduct3", 3.0, "desc3", "https://example.com/image3.jpg", 0),
                new Product(4L, "CachedProduct4", 4.0, "desc4", "https://example.com/image4.jpg", 0)
        );

        when(productRepository.findAll()).thenReturn(Flux.fromIterable(products));

        List<Product> listFromDB = underTest
                .getProducts("", "ALPHA", 1, 3)
                .collectList()
                .block();

        assertThat(listFromDB)
                .isNotNull()
                .hasSize(3)
                .extracting(Product::getName)
                .containsExactly("CachedProduct1", "CachedProduct2", "CachedProduct3");
        verify(productRepository, times(1)).findAll();

        // Fetching the products from the Redis cache
        when(productRepository.findAll()).thenReturn(Flux.empty());
        List<Product> listFromCache = underTest
                .getProducts("", "ALPHA", 1, 3)
                .collectList()
                .block();

        assertThat(listFromCache)
                .isNotNull()
                .hasSize(3)
                .extracting(Product::getName)
                .containsExactly("CachedProduct1", "CachedProduct2", "CachedProduct3");

        verify(productRepository, times(1)).findAll();
    }

}

