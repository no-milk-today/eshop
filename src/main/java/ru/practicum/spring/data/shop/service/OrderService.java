package ru.practicum.spring.data.shop.service;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import ru.practicum.spring.data.shop.domain.entity.Order;
import ru.practicum.spring.data.shop.repository.OrderRepository;

import java.util.List;
import java.util.Optional;

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
}
