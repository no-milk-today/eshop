package com.yandex.reactive.testcontainers.testconainers_demo_raactive;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ProductRepositoryTest extends AbstractTestContainerTest {

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
                        AssertionsForClassTypes.assertThat(productFromDB)
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
                                    AssertionsForClassTypes.assertThat(foundProduct)
                                            .withFailMessage("Found product should not be null")
                                            .isNotNull();
                                    AssertionsForClassTypes.assertThat(foundProduct.getName())
                                            .withFailMessage("Product name should match")
                                            .isEqualTo("Test product findById");
                                })
                )
                .block();
    }
}
