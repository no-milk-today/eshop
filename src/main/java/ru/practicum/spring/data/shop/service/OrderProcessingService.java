package ru.practicum.spring.data.shop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.spring.data.shop.domain.entity.Cart;
import ru.practicum.spring.data.shop.domain.entity.Order;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderProcessingService {

    private final OrderService orderService;
    private final CartService cartService;

    public OrderProcessingService(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    @Transactional
    public Order processOrder() {
        Cart cart = cartService.getCart();
        log.debug("Cart retrieved: {}", cart);

        if (cart.getProducts().isEmpty()) {
            log.warn("Cart is empty, cannot process order");
            throw new ResourceNotFoundException("Cart is empty");
        }

        // Retrieve counts and update each product's count from the cart
        Map<Long, Integer> counts = cartService.getProductCounts();
        List<Product> products = cart.getProducts().stream()
                .peek(p -> p.setCount(counts.getOrDefault(p.getId(), 0)))
                .collect(Collectors.toList());
        log.debug("Product retrieved: {}, Counts: {}", products, counts);

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setProducts(products);
        order.setTotalSum(cart.getTotalPrice());
        order.setOrderDate(LocalDateTime.now());
        order = orderService.save(order);
        log.info("Order saved: {}", order);
        cartService.clearCart();
        log.debug("Cart cleared after order processing");
        return order;
    }
}