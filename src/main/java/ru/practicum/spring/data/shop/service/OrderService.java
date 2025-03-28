package ru.practicum.spring.data.shop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import ru.practicum.spring.data.shop.domain.entity.Order;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.repository.OrderRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Optional<Order> findById(Long id) {
        log.debug("Find order with id: {}", id);
        return orderRepository.findById(id);
    }

    public List<Order> findAll() {
        log.debug("Retrieving all orders");
        return orderRepository.findAll();
    }

    public List<Order> findAllSorted(Sort sort) {
        log.debug("Retrieving all orders sorted by: {}", sort);
        return orderRepository.findAll(sort);
    }

    public Order save(Order order) {
        log.info("Saving order: {}", order);
        return orderRepository.save(order);
    }

    public void deleteById(Long id) {
        log.info("Deleting order with id: {}", id);
        orderRepository.deleteById(id);
        log.info("Order with id {} deleted", id);
    }

    public double calculateTotalSum(Order order) {
        double totalSum = order.getProducts().stream()
                .mapToDouble(product -> product.getPrice() * product.getCount())
                .sum();
        log.debug("Calculated total sum for order with id {}: {}", order.getId(), totalSum);
        return totalSum;
    }

    /**
     * Фетчит уникальные продукты и их количество
     *
     * @param products initial products
     * @return уникальные продукты и их количество
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