package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Order;
import com.yandex.reactive.testcontainers.reshop.domain.entity.OrderProduct;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.exception.ResourceNotFoundException;
import com.yandex.reactive.testcontainers.reshop.repository.OrderProductRepository;
import com.yandex.reactive.testcontainers.reshop.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepository,
                        OrderProductRepository orderProductRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.orderProductRepository = orderProductRepository;
        this.productService = productService;
    }

    public Mono<Order> findById(Long id) {
        log.debug("Find order with id: {}", id);
        return orderRepository.findById(id);
    }

    public Flux<Order> findAll() {
        log.debug("Retrieving all orders");
        return orderRepository.findAll();
    }

    public Flux<Order> findAllSorted(Sort sort) {
        log.debug("Retrieving all orders sorted by: {}", sort);
        return orderRepository.findAll(sort);
    }

    /**
     * Сохраняет заказ и регистрирует связи с продуктами в таблице order_products.
     *
     * @param order заказ с заполненным списком продуктов (order.getProducts())
     * @return Mono<Order> с сохранённым заказом
     */
    public Mono<Order> save(Order order) {
        log.info("Saving order: {}", order);
        return orderRepository.save(order)
                .flatMap(savedOrder -> {
                    log.debug("Order saved with id: {}", savedOrder.getId());
                    List<Product> products = order.getProducts();
                    if (products == null || products.isEmpty()) {
                        return Mono.just(savedOrder);
                    }
                    // Для каждого продукта сохраняем запись в таблице order_products
                    return Flux.fromIterable(products)
                            .flatMap(product ->
                                    orderProductRepository.save(
                                            new OrderProduct(null, savedOrder.getId(), product.getId())
                                    )
                            )
                            .then(Mono.just(savedOrder));
                });
    }

    public Mono<Void> deleteById(Long id) {
        log.info("Deleting order with id: {}", id);
        return orderRepository.deleteById(id)
                .doOnSuccess(v -> log.info("Order with id {} deleted", id));
    }

    /**
     * Рассчитывает итоговую сумму заказа.
     * Умножает цену каждого продукта на его count и суммирует.
     *
     * @param order с заполненным списком продуктов
     * @return итоговая сумма
     */
    public double calculateTotalSum(Order order) {
        double totalSum = order.getProducts().stream()
                .mapToDouble(product -> product.getPrice() * product.getCount())
                .sum();
        log.debug("Calculated total sum for order with id {}: {}", order.getId(), totalSum);
        return totalSum;
    }

    /**
     * Фетчит уникальные продукты и их количество.
     * Работает синхронно.
     *
     * @param products исходный список продуктов
     * @return список уникальных продуктов с установленным количеством
     */
    public List<Product> groupProductsWithCounts(List<Product> products) {
        if (products == null) {
            log.error("Grouping 0 products (products list is null)");
        }
        log.debug("Grouping {} products", products.size());
        Map<Long, Integer> productCounts = products.stream()
                .collect(Collectors.groupingBy(Product::getId, Collectors.summingInt(p -> 1)));
        return products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> p,
                        (p1, p2) -> p1
                ))
                .values().stream()
                .peek(p -> p.setCount(productCounts.getOrDefault(p.getId(), 0)))
                .collect(Collectors.toList());
    }

    public Mono<Order> findByIdWithProducts(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order with id [" + id + "] not found")))
                .flatMap(order ->
                        orderProductRepository.findByOrderId(order.getId())
                                .flatMap(op -> productService.findById(op.getProductId()))
                                .collectList()
                                .map(products -> {
                                    order.setProducts(products);
                                    return order;
                                })
                );
    }
}

