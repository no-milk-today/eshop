package ru.practicum.spring.data.shop.service;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.practicum.spring.data.shop.domain.entity.Order;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        when(orderRepository.findById(1L)).thenReturn(Optional.of(expectedOrder));

        var orderOpt = underTest.findById(1L);
        assertTrue(orderOpt.isPresent());
        assertEquals(expectedOrder.getId(), orderOpt.get().getId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void testFindAll() {
        var order1 = new Order();
        order1.setId(1L);
        var order2 = new Order();
        order2.setId(2L);
        var expectedList = List.of(order1, order2);
        when(orderRepository.findAll()).thenReturn(expectedList);

        var orders = underTest.findAll();
        assertNotNull(orders);
        assertEquals(2, orders.size());
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
        when(orderRepository.findAll(sort)).thenReturn(expectedList);

        var orders = underTest.findAllSorted(sort);
        assertNotNull(orders);
        assertEquals(2, orders.size());
        verify(orderRepository, times(1)).findAll(sort);
    }

    @Test
    void testSave() {
        var order = new Order();
        order.setId(5L);
        order.setOrderDate(LocalDateTime.now());
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        var savedOrder = underTest.save(order);
        assertNotNull(savedOrder);
        assertEquals(5L, savedOrder.getId());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void testDeleteById() {
        long id = 6L;
        doNothing().when(orderRepository).deleteById(id);

        underTest.deleteById(id);
        verify(orderRepository, times(1)).deleteById(id);
    }

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
        assertEquals(40.0, total);
    }

    @Test
    void testGroupProductsWithCounts() {
        var products = getProductList();
        List<Product> groupedProducts = underTest.groupProductsWithCounts(products);

        assertNotNull(groupedProducts);
        assertEquals(2, groupedProducts.size());

        for (Product product : groupedProducts) {
            if (product.getId().equals(1L)) {
                assertEquals(2, product.getCount(), "Grouped product for id 1 should have count 2");
            } else if (product.getId().equals(2L)) {
                assertEquals(1, product.getCount(), "Grouped product for id 2 should have count 1");
            }
        }
    }

    private static @NotNull List<Product> getProductList() {
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