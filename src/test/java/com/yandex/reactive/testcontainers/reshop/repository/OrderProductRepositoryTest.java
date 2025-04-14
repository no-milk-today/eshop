package com.yandex.reactive.testcontainers.reshop.repository;


import com.yandex.reactive.testcontainers.reshop.AbstractDaoTest;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Order;
import com.yandex.reactive.testcontainers.reshop.domain.entity.OrderProduct;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.domain.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderProductRepositoryTest extends AbstractDaoTest {

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User createTestUser() {
        var user = new User();
        user.setUsername("testUser_" + System.currentTimeMillis()); // чтоб без дубликатов
        user.setPassword("password");
        user.setEmail("test_" + System.currentTimeMillis() + "@example.com");
        return userRepository.save(user).block();
    }

    private Order createTestOrder() {
        var testUser = createTestUser();
        var order = new Order();
        order.setUserId(testUser.getId());
        order.setOrderDate(LocalDateTime.now());
        order.setNumber("123");
        order.setTotalSum(0.0);
        return orderRepository.save(order).block();
    }

    private Product createTestProduct() {
        var product = new Product();
        product.setName("TestProduct");
        product.setPrice(9.99);
        product.setDescription("Test product");
        product.setImgPath("http://example.com/test.jpg");
        return productRepository.save(product).block();
    }

    @Test
    void testSaveOrderProductAndFindByOrderId() {
        var order = createTestOrder();
        var product = createTestProduct();
        var orderProduct = new OrderProduct(null, order.getId(), product.getId());
        var orderProductFromDB = orderProductRepository.save(orderProduct).block();
        assertThat(orderProductFromDB).isNotNull();
        assertThat(orderProductFromDB.getId()).isNotNull();
        assertThat(orderProductFromDB.getOrderId()).isEqualTo(order.getId());
        assertThat(orderProductFromDB.getProductId()).isEqualTo(product.getId());

        var orderProducts = orderProductRepository.findByOrderId(order.getId())
                .collectList()
                .block();
        assertThat(orderProducts).isNotNull();
        assertThat(orderProducts).isNotEmpty();
        OrderProduct first = orderProducts.get(0);
        assertThat(first.getProductId()).isEqualTo(product.getId());
    }

    @Test
    void testFindByOrderIdAndProductId() {
        var order = createTestOrder();
        var product = createTestProduct();
        var orderProduct = new OrderProduct(null, order.getId(), product.getId());
        orderProductRepository.save(orderProduct).block();

        var orderProducts = orderProductRepository.findByOrderIdAndProductId(order.getId(), product.getId())
                .collectList()
                .block();
        assertThat(orderProducts).isNotNull();
        assertThat(orderProducts).hasSize(1);
        OrderProduct retrieved = orderProducts.get(0);
        assertThat(retrieved.getOrderId()).isEqualTo(order.getId());
        assertThat(retrieved.getProductId()).isEqualTo(product.getId());
    }
}