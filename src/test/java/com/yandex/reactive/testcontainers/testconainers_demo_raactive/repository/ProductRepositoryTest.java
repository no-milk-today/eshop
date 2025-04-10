package com.yandex.reactive.testcontainers.testconainers_demo_raactive.repository;

import com.yandex.reactive.testcontainers.testconainers_demo_raactive.AbstractDaoTest;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.Product;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
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

    @Test
    void testFindAllProductsWithPagination() {
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            var product = new Product();
            product.setName("Product " + i);
            product.setPrice(10.0 * i);
            product.setDescription("Description " + i);
            product.setImgPath("https://example.com/image" + i + ".jpg");
            products.add(product);
        }

        // Сохраняем все продукты
        var savedProductsFlux = Flux.fromIterable(products)
                .flatMap(underTest::save);

        savedProductsFlux.collectList()
                .flatMap(savedList -> {
                    int limit = 2;
                    int offset = 0;
                    return underTest.findAllProducts(limit, offset).collectList()
                            .flatMap(paginatedProducts -> {
                                assertThat(paginatedProducts)
                                        .withFailMessage("Expected 2 products on the first page")
                                        .hasSize(2);
                                return underTest.countProducts()
                                        .map(totalCount -> {
                                            // Проверяем, что общее количество продуктов как минимум равно количеству сохраненных
                                            assertThat(totalCount)
                                                    .withFailMessage("Total count should be at least the number of saved products")
                                                    .isGreaterThanOrEqualTo((long) savedList.size());
                                            return paginatedProducts;
                                        });
                            });
                })
                .block();
    }

    @Test
    void testSearchProductsWithPagination() {
        var product1 = new Product();
        product1.setName("Unique Product");
        product1.setPrice(15.0);
        product1.setDescription("This is a unique product description");
        product1.setImgPath("https://example.com/unique.jpg");

        var product2 = new Product();
        product2.setName("Another Product");
        product2.setPrice(20.0);
        product2.setDescription("This product is quite unique in features");
        product2.setImgPath("https://example.com/another.jpg");

        var product3 = new Product();
        product3.setName("Regular Product");
        product3.setPrice(25.0);
        product3.setDescription("Regular product description");
        product3.setImgPath("https://example.com/regular.jpg");

        // Сохраняем продактс
        Flux<Product> savedFlux = Flux.just(product1, product2, product3)
                .flatMap(each -> underTest.save(each));

        savedFlux.collectList()
                .flatMap(savedList -> {
                    // поиск по строке "unique" с limit 10 и offset 0
                    int limit = 10;
                    int offset = 0;
                    return underTest.searchProducts("unique", limit, offset).collectList()
                            .flatMap(searchResults -> {
                                // Ожидаем, что найдутся продукты, содержащие "unique" (учитывается как в имени, так и в описании)
                                assertThat(searchResults)
                                        .withFailMessage("Search results should not be empty")
                                        .isNotEmpty();
                                return underTest.countProductsBySearch("unique")
                                        .map(searchCount -> {
                                            assertThat(searchCount)
                                                    .withFailMessage("Count from search should equal the number of search results")
                                                    .isEqualTo((long) searchResults.size());
                                            return searchResults;
                                        });
                            });
                })
                .block();
    }
}
