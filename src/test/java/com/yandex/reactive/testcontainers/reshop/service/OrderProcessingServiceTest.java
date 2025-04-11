package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Cart;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Order;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.domain.entity.User;
import com.yandex.reactive.testcontainers.reshop.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderProcessingService underTest;

    @Test
    void testProcessOrder_success() {
        var cart = new Cart();
        var user = new User();
        user.setId(1L);
        cart.setUserId(user.getId());

        var product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(10.0);
        product.setCount(0);
        cart.setProducts(List.of(product));
        cart.setTotalPrice(10.0);

        when(cartService.getCart()).thenReturn(Mono.just(cart));

        Map<Long, Integer> counts = new HashMap<>();
        counts.put(1L, 1);
        when(cartService.getProductCounts()).thenReturn(Mono.just(counts));

        var orderFromDB = new Order();
        orderFromDB.setId(7L);
        orderFromDB.setUserId(user.getId());
        orderFromDB.setProducts(List.copyOf(cart.getProducts()));
        orderFromDB.setTotalSum(cart.getTotalPrice());
        orderFromDB.setOrderDate(LocalDateTime.now());
        when(orderService.save(any(Order.class))).thenReturn(Mono.just(orderFromDB));

        when(cartService.clearCart()).thenReturn(Mono.empty());

        Order order = underTest.processOrder().block();

        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(7L);
        verify(orderService).save(any(Order.class));
        verify(cartService).clearCart();
    }

    @Test
    void processOrder_ResourceNotFoundException() {
        var emptyCart = new Cart();
        emptyCart.setProducts(List.of());
        when(cartService.getCart()).thenReturn(Mono.just(emptyCart));

        assertThrows(ResourceNotFoundException.class, () -> underTest.processOrder().block());
        verify(orderService, never()).save(any(Order.class));
        verify(cartService, never()).clearCart();
    }
}
