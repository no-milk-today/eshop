package ru.practicum.spring.data.shop.service;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import ru.practicum.spring.data.shop.domain.entity.Order;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.repository.OrderRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public List<Order> findAllSorted(Sort sort) {
        return orderRepository.findAll(sort);
    }

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }

    public double calculateTotalSum(Order order) {
        return order.getProducts().stream()
                .mapToDouble(product -> product.getPrice() * product.getCount())
                .sum();
    }

    /**
     * Фетчит уникальные продукты и их количество
     *
     * @param products initial products
     * @return уникальные продукты и их количество
     */
    public List<Product> groupProductsWithCounts(List<Product> products) {
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