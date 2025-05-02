package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.AbstractTestContainerTest;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

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

}

