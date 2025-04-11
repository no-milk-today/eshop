package com.yandex.reactive.testcontainers.testconainers_demo_raactive.service;

import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.Order;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.Product;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.repository.OrderRepository;
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

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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

    public Mono<Order> save(Order order) {
        log.info("Saving order: {}", order);
        return orderRepository.save(order);
    }

    public Mono<Void> deleteById(Long id) {
        log.info("Deleting order with id: {}", id);
        return orderRepository.deleteById(id)
                .doOnSuccess(v -> log.info("Order with id {} deleted", id));
    }

    /**
     * Рассчитывает общую сумму заказа.
     * Работает синхронно, принимает уже загруженный Order.
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
}

