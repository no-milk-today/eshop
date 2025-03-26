package ru.practicum.spring.data.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.spring.data.shop.domain.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
