package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Order;
import com.yandex.reactive.testcontainers.reshop.exception.PaymentException;
import com.yandex.reactive.testcontainers.reshop.exception.ResourceNotFoundException;
import com.yandex.reactive.testcontainers.reshop.repository.CartProductRepository;
import com.yandex.reactive.testcontainers.reshop.repository.OrderProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderProcessingService {

    private final OrderService orderService;
    private final CartService cartService;
    private final ProductService productService;
    private final OrderProductRepository orderProductRepository;
    private final CartProductRepository cartProductRepository;
    private final PaymentClientService paymentClientService;

    public OrderProcessingService(OrderService orderService,
                                  CartService cartService,
                                  ProductService productService,
                                  OrderProductRepository orderProductRepository,
                                  CartProductRepository cartProductRepository,
                                  PaymentClientService paymentClientService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.productService = productService;
        this.orderProductRepository = orderProductRepository;
        this.cartProductRepository = cartProductRepository;
        this.paymentClientService = paymentClientService;
    }

    /**
     * Обрабатывает заказ
     * - Получает корзину.
     * - Из join-таблицы (cart_products) получает все записи для корзины. Если корзина пуста – выдаёт ошибку.
     * - Группирует записи по productId чтобы подсчитать count каждого продукта.
     * - Загружает unique продукты и устанавливает их count.
     * - Формирует Order
     * - Делает rest call в payment с помощью PaymentClientService.
     * - Сохраняет заказ (в OrderService после сохранения вставляются записи в order_products).
     * - Очищает корзину.
     */
    public Mono<Order> processOrder() {
        return cartService.getCart()
                .flatMap(cart ->
                        cartProductRepository.findByCartId(cart.getId())  // use cartProductRepository here
                                .collectList()
                                .flatMap(cartProducts -> {
                                    if (cartProducts.isEmpty()) {
                                        log.warn("Cart is empty, cannot process order");
                                        return Mono.error(new ResourceNotFoundException("Cart is empty"));
                                    }
                                    Map<Long, Integer> counts = cartProducts.stream()
                                            .collect(Collectors.groupingBy(
                                                    cp -> cp.getProductId(),
                                                    Collectors.summingInt(cp -> 1)
                                            ));
                                    List<Long> productIds = cartProducts.stream()
                                            .map(cp -> cp.getProductId())
                                            .distinct()
                                            .collect(Collectors.toList());
                                    return Flux.fromIterable(productIds)
                                            .flatMap(productService::findById)
                                            .collectList()
                                            .map(products -> {
                                                products.forEach(p -> p.setCount(counts.getOrDefault(p.getId(), 0)));
                                                return products;
                                            })
                                            .flatMap(products -> {
                                                log.debug("Products retrieved: {}, with counts: {}", products, counts);
                                                Order order = new Order();
                                                order.setUserId(cart.getUserId());
                                                order.setProducts(products);
                                                order.setTotalSum(cart.getTotalPrice());
                                                order.setOrderDate(LocalDateTime.now());
                                                // Call remote payment service before saving order.
                                                return paymentClientService.makePayment(String.valueOf(cart.getUserId()), cart.getTotalPrice())
                                                        .flatMap(paymentSuccess -> {
                                                            if (!paymentSuccess) {
                                                                return Mono.error(new PaymentException("Payment failed due to processing error."));
                                                            }
                                                            return orderService.save(order)
                                                                    .flatMap(savedOrder ->
                                                                            cartService.clearCart().thenReturn(savedOrder)
                                                                    );
                                                        });
                                            });
                                })
                )
                .doOnSuccess(order -> log.info("Order saved: {}", order));
    }
}
