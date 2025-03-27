package ru.practicum.spring.data.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

@Test
void testGroupProducts() {
    // Create 5 products manually with filled-in data.
    var product1 = new Product();
    product1.setName("Test product 1");
    product1.setPrice(9.99);
    product1.setDescription("This is a test product 1");
    product1.setImgPath("https://example.com/tshirt_2025_1.jpg");
    product1.setCount(100);

    var product2 = new Product();
    product2.setName("Test product 2");
    product2.setPrice(9.99);
    product2.setDescription("This is a test product 2");
    product2.setImgPath("https://example.com/tshirt_2025_2.jpg");
    product2.setCount(100);

    var product3 = new Product();
    product3.setName("Test product 3");
    product3.setPrice(9.99);
    product3.setDescription("This is a test product 3");
    product3.setImgPath("https://example.com/tshirt_2025_3.jpg");
    product3.setCount(100);

    var product4 = new Product();
    product4.setName("Test product 4");
    product4.setPrice(9.99);
    product4.setDescription("This is a test product 4");
    product4.setImgPath("https://example.com/tshirt_2025_4.jpg");
    product4.setCount(100);

    var product5 = new Product();
    product5.setName("Test product 5");
    product5.setPrice(9.99);
    product5.setDescription("This is a test product 5");
    product5.setImgPath("https://example.com/tshirt_2025_5.jpg");
    product5.setCount(100);

    var products = List.of(product1, product2, product3, product4, product5);

    var grouped = productService.groupProducts(products);

    assertThat(grouped).hasSize(2);
    assertThat(grouped.get(0)).hasSize(3);
    assertThat(grouped.get(1)).hasSize(2);
}

    @Test
    void testGetProductsWithoutSearchAndSort() {
        // Подготовим "пустой" поиск и сортировку NO
        List<Product> productList = List.of(new Product(), new Product());
        Page<Product> page = new PageImpl<>(productList, PageRequest.of(0, 10, Sort.unsorted()), productList.size());
        when(productRepository.findAll(PageRequest.of(0, 10, Sort.unsorted()))).thenReturn(page);

        Page<Product> result = productService.getProducts("", "NO", 1, 10);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void testGetProductsWithAlphaSort() {
        List<Product> productList = List.of(new Product(), new Product());
        Page<Product> page = new PageImpl<>(productList, PageRequest.of(0, 10, Sort.by("name").ascending()), productList.size());
        when(productRepository.findAll(PageRequest.of(0, 10, Sort.by("name").ascending()))).thenReturn(page);

        Page<Product> result = productService.getProducts("", "ALPHA", 1, 10);
        assertThat(result.getContent()).hasSize(2);
    }
}