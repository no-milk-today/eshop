package ru.practicum.spring.data.shop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.spring.data.shop.domain.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Searches for products by name or description, case-insensitive.
     *
     * @param name the search string for the product name.
     * @param description the search string for the product description.
     * @param pageable pagination information.
     * @return a page of matching products.
     */
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description, Pageable pageable);
}
