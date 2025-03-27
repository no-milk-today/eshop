package ru.practicum.spring.data.shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.spring.data.shop.domain.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Поиск продуктов по имени или описанию, case insensitive.
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description, Pageable pageable);
}
