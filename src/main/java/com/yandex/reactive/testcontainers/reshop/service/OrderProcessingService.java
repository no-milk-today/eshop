package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Order;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
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
    public Mono<Order> processOrder() {
        return cartService.getCart()
                .flatMap(cart -> {
                    log.debug("Cart retrieved: {}", cart);

                    if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
                        log.warn("Cart is empty, cannot process order");
                        return Mono.error(new ResourceNotFoundException("Cart is empty"));
                    }
                    // Получаем Cart с количеством товаров
                    return cartService.getProductCounts()
                            .flatMap(counts -> {
                                // Обновляем для каждого продукта его количество
                                List<Product> products = cart.getProducts().stream()
                                        .peek(p -> p.setCount(counts.getOrDefault(p.getId(), 0)))
                                        .collect(Collectors.toList());
                                log.debug("Products retrieved: {}, Counts: {}", products, counts);

                                Order order = new Order();
                                order.setUserId(cart.getUserId());
                                order.setProducts(products);
                                order.setTotalSum(cart.getTotalPrice());
                                order.setOrderDate(LocalDateTime.now());
                                return orderService.save(order)
                                        .flatMap(savedOrder ->
                                                cartService.clearCart().thenReturn(savedOrder)
                                        );
                            });
                })
                .doOnSuccess(order -> log.info("Order processed successfully: {}", order))
                .doOnError(e -> log.error("Error processing order: {}", e.getMessage()));
    }
}
