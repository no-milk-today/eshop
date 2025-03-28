package ru.practicum.spring.data.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.spring.data.shop.domain.entity.Cart;
import ru.practicum.spring.data.shop.domain.entity.Order;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.domain.entity.User;
import ru.practicum.spring.data.shop.exception.ResourceNotFoundException;

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
        cart.setUser(user);
        var product = new Product(1L, "Test Product", 10.0, "Description", "img.jpg", 1);
        cart.setProducts(List.of(product));
        cart.setTotalPrice(10.0);

        when(cartService.getCart()).thenReturn(cart);

        var orderFromDB = new Order();
        orderFromDB.setId(7L);
        orderFromDB.setUser(user);
        orderFromDB.setProducts(List.copyOf(cart.getProducts()));
        orderFromDB.setTotalSum(cart.getTotalPrice());
        orderFromDB.setOrderDate(LocalDateTime.now());

        when(orderService.save(any(Order.class))).thenReturn(orderFromDB);

        var order = underTest.processOrder();

        assertNotNull(order);
        assertEquals(7L, order.getId());
        verify(orderService).save(any(Order.class));
        verify(cartService).clearCart();
    }

    @Test
    void processOrder_ResourceNotFoundException() {
        var emptyCart = new Cart();
        emptyCart.setProducts(List.of());
        when(cartService.getCart()).thenReturn(emptyCart);

        assertThrows(ResourceNotFoundException.class, () -> underTest.processOrder());

        verify(orderService, never()).save(any(Order.class));
        verify(cartService, never()).clearCart();
    }
}