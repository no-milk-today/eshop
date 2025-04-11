package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Order;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService underTest;

    @Test
    void testFindById() {
        var expectedOrder = new Order();
        expectedOrder.setId(1L);
        expectedOrder.setOrderDate(LocalDateTime.now());
        expectedOrder.setTotalSum(100.0);
        when(orderRepository.findById(1L)).thenReturn(Mono.just(expectedOrder));

        var result = underTest.findById(1L).block();
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(expectedOrder.getId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void testFindAll() {
        var order1 = new Order();
        order1.setId(1L);
        var order2 = new Order();
        order2.setId(2L);
        var expectedList = List.of(order1, order2);
        when(orderRepository.findAll()).thenReturn(Flux.fromIterable(expectedList));

        var orders = underTest.findAll().collectList().block();
        assertThat(orders).isNotNull();
        assertThat(orders).hasSize(2);
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void testFindAllSorted() {
        var order1 = new Order();
        order1.setId(3L);
        var order2 = new Order();
        order2.setId(4L);
        var expectedList = List.of(order1, order2);
        var sort = Sort.by(Sort.Direction.ASC, "id");
        when(orderRepository.findAll(sort)).thenReturn(Flux.fromIterable(expectedList));

        var orders = underTest.findAllSorted(sort).collectList().block();
        assertThat(orders).isNotNull();
        assertThat(orders).hasSize(2);
        verify(orderRepository, times(1)).findAll(sort);
    }

    @Test
    void testSave() {
        var order = new Order();
        order.setId(5L);
        order.setOrderDate(LocalDateTime.now());
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));

        var savedOrder = underTest.save(order).block();
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getId()).isEqualTo(5L);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void testDeleteById() {
        long id = 6L;
        when(orderRepository.deleteById(id)).thenReturn(Mono.empty());

        underTest.deleteById(id).block();
        verify(orderRepository, times(1)).deleteById(id);
    }

    /**
     * Методы calculateTotalSum и groupProductsWithCounts синхронные,
     * т.к. они работают с уже полученными данными.
     */
    @Test
    void testCalculateTotalSum() {
        var product1 = new Product();
        product1.setId(1L);
        product1.setPrice(10.0);
        product1.setCount(2);
        var product2 = new Product();
        product2.setId(2L);
        product2.setPrice(20.0);
        product2.setCount(1);

        var order = new Order();
        order.setProducts(List.of(product1, product2));

        double total = underTest.calculateTotalSum(order);
        assertThat(total).isEqualTo(40.0);
    }

    @Test
    void testGroupProductsWithCounts() {
        var products = getProductList();
        List<Product> groupedProducts = underTest.groupProductsWithCounts(products);

        assertThat(groupedProducts).isNotNull();
        assertThat(groupedProducts).hasSize(2);

        for (Product product : groupedProducts) {
            if (product.getId().equals(1L)) {
                assertThat(product.getCount()).isEqualTo(2);
            } else if (product.getId().equals(2L)) {
                assertThat(product.getCount()).isEqualTo(1);
            }
        }
    }

    private static List<Product> getProductList() {
        var product1a = new Product();
        product1a.setId(1L);
        product1a.setName("Product A");
        product1a.setPrice(10.0);
        product1a.setCount(0);

        var product1b = new Product();
        product1b.setId(1L);
        product1b.setName("Product A");
        product1b.setPrice(10.0);
        product1b.setCount(0);

        var product2 = new Product();
        product2.setId(2L);
        product2.setName("Product B");
        product2.setPrice(20.0);
        product2.setCount(0);

        return List.of(product1a, product1b, product2);
    }
}
