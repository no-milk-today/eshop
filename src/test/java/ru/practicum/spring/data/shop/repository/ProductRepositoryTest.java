package ru.practicum.spring.data.shop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.spring.data.shop.domain.entity.Product;

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

        var productFromDB = underTest.save(product);
        assertThat(productFromDB.getId()).isNotNull();
    }
}