package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Order;
import com.yandex.reactive.testcontainers.reshop.domain.entity.OrderProduct;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.repository.OrderProductRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderProductRepository orderProductRepository;

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
    void testSave_OrderWithoutProducts() {
        var order = new Order();
        order.setOrderDate(LocalDateTime.now());
        order.setTotalSum(0.0);
        order.setProducts(null);

        var orderFromDB = new Order();
        orderFromDB.setId(5L);
        orderFromDB.setOrderDate(order.getOrderDate());
        orderFromDB.setTotalSum(order.getTotalSum());

        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(orderFromDB));

        var result = underTest.save(order).block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
        verify(orderRepository, times(1)).save(order);
        verify(orderProductRepository, never()).save(any(OrderProduct.class));
    }

    @Test
    void testSave_OrderWithProducts() {
        var product = new Product();
        product.setId(10L);
        product.setPrice(20.0);
        orderSetProductCount(product, 2);

        var order = new Order();
        order.setOrderDate(LocalDateTime.now());
        order.setTotalSum(0.0);
        order.setProducts(List.of(product));

        var orderFromDB = new Order();
        orderFromDB.setId(6L);
        orderFromDB.setOrderDate(order.getOrderDate());
        orderFromDB.setTotalSum(order.getTotalSum());
        orderFromDB.setProducts(order.getProducts());

        var orderProduct = new OrderProduct(null, orderFromDB.getId(), product.getId());

        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(orderFromDB));
        when(orderProductRepository.save(any(OrderProduct.class))).thenReturn(Mono.just(orderProduct));

        var result = underTest.save(order).block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(6L);
        verify(orderRepository, times(1)).save(order);
        verify(orderProductRepository, times(1)).save(any(OrderProduct.class));
    }

    @Test
    void testDeleteById() {
        long id = 7L;
        when(orderRepository.deleteById(id)).thenReturn(Mono.empty());

        underTest.deleteById(id).block();
        verify(orderRepository, times(1)).deleteById(id);
    }

    @Test
    void testCalculateTotalSum() {
        var product1 = new Product();
        product1.setId(1L);
        product1.setPrice(10.0);
        orderSetProductCount(product1, 2);

        var product2 = new Product();
        product2.setId(2L);
        product2.setPrice(20.0);
        orderSetProductCount(product2, 1);

        var order = new Order();
        order.setProducts(List.of(product1, product2));

        double total = underTest.calculateTotalSum(order);
        assertThat(total).isEqualTo(40.0);
    }

    @Test
    void testGroupProductsWithCounts() {
        var product1a = new Product();
        product1a.setId(1L);
        product1a.setPrice(10.0);
        product1a.setCount(0);

        var product1b = new Product();
        product1b.setId(1L);
        product1b.setPrice(10.0);
        product1b.setCount(0);

        var product2 = new Product();
        product2.setId(2L);
        product2.setPrice(20.0);
        product2.setCount(0);

        List<Product> products = List.of(product1a, product1b, product2);
        List<Product> grouped = underTest.groupProductsWithCounts(products);

        assertThat(grouped).isNotNull();
        assertThat(grouped).hasSize(2);

        for (Product p : grouped) {
            if (p.getId().equals(1L)) {
                assertThat(p.getCount()).isEqualTo(2);
            } else if (p.getId().equals(2L)) {
                assertThat(p.getCount()).isEqualTo(1);
            }
        }
    }

    // Helper method для count продукта через сеттер (count не сохраняется в маппингах Order/Product)
    private void orderSetProductCount(Product product, int count) {
        product.setCount(count);
    }
}