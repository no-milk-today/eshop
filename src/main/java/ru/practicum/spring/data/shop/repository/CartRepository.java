package ru.practicum.spring.data.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.spring.data.shop.domain.entity.Cart;
import ru.practicum.spring.data.shop.domain.entity.User;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
}