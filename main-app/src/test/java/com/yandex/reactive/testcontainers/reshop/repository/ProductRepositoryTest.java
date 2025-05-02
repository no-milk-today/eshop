package com.yandex.reactive.testcontainers.reshop.repository;

import com.yandex.reactive.testcontainers.reshop.AbstractDaoTest;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductRepositoryTest extends AbstractDaoTest {

    @Autowired
    private ProductRepository underTest;

    @Test
    void testCreateProduct() {
        var product = new Product();
        product.setName("Test product");
        product.setPrice(9.99);
        product.setDescription("This is a test product");
        product.setImgPath("https://example.com/tshirt_2025.jpg");

        underTest.save(product)
                .doOnNext(productFromDB ->
                        assertThat(productFromDB)
                                .withFailMessage("Result of save should not be null")
                                .isNotNull()
                                .extracting(Product::getId)
                                .withFailMessage("Saved product should have an ID assigned")
                                .isNotNull()
                )
                .block();
    }

    @Test
    void testFindById() {
        var product = new Product();
        product.setName("Test product findById");
        product.setPrice(9.99);
        product.setDescription("Test description findById");
        product.setImgPath("https://example.com/test_image.jpg");

        underTest.save(product)
                .flatMap(productFromDB ->
                        underTest.findById(productFromDB.getId())
                                .doOnNext(foundProduct -> {
                                    assertThat(foundProduct)
                                            .withFailMessage("Found product should not be null")
                                            .isNotNull();
                                    assertThat(foundProduct.getName())
                                            .withFailMessage("Product name should match")
                                            .isEqualTo("Test product findById");
                                })
                )
                .block();
    }

    @Test
    void testFindByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase() {
        var product1 = new Product();
        product1.setName("Awesome T-shirt");
        product1.setPrice(10.0);
        product1.setDescription("High quality cotton t-shirt");
        product1.setImgPath("http://example.com/tshirt.jpg");

        var product2 = new Product();
        product2.setName("Cool Hat");
        product2.setPrice(15.0);
        product2.setDescription("Stylish summer hat");
        product2.setImgPath("http://example.com/hat.jpg");

        var product3 = new Product();
        product3.setName("Regular Shoes");
        product3.setPrice(20.0);
        product3.setDescription("Comfortable running shoes");
        product3.setImgPath("http://example.com/shoes.jpg");

        Mono<Product> mono1 = underTest.save(product1);
        Mono<Product> mono2 = underTest.save(product2);
        Mono<Product> mono3 = underTest.save(product3);

        // все три products сохранены в БД до выполнения findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase
        Flux.merge(mono1, mono2, mono3)
                .collectList()
                .block();

        List<Product> products = underTest
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase("t-shirt", "t-shirt")
                .collectList()
                .block();

        assertThat(products).isNotEmpty();
        assertThat(products)
                .anyMatch(product -> product.getName().equalsIgnoreCase("Awesome T-shirt"));
    }
}