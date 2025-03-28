package ru.practicum.spring.data.shop.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.domain.enums.ProductSort;
import ru.practicum.spring.data.shop.repository.ProductRepository;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    // Количество товаров в одном ряду плитки (может быть вынесено в конфигурацию)
    private static final int ITEMS_PER_ROW = 3;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Searches for products considering search, sorting, and pagination.
     * Parameters:
     *   search - search string for name or description
     *   sort - sorting: "NO" (no sorting), "ALPHA" (by name), "PRICE" (by price)
     *   pageNumber - page number (1-based)
     *   pageSize - page size
     */
    public Page<Product> getProducts(String search, String sort, int pageNumber, int pageSize) {
        log.debug("Fetching products with search: '{}', sort: '{}', pageNumber: {}, pageSize: {}",
                search, sort, pageNumber, pageSize);
        ProductSort sortType = ProductSort.from(sort);
        Sort sortOrder = switch (sortType) {
            case ALPHA -> Sort.by("name").ascending();
            case PRICE -> Sort.by("price").ascending();
            default -> Sort.unsorted();
        };
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sortOrder);

        if (search == null || search.trim().isEmpty()) {
            return productRepository.findAll(pageable);
        } else {
            return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
        }
    }

    /**
     * Splits the list of products into groups of ITEMS_PER_ROW for tile display.
     *
     * @param products list of products
     * @return list of rows, where each row is a list of products in one row
     */
    public List<List<Product>> groupProducts(List<Product> products) {
        log.debug("Grouping {} products into rows of {} items each", products.size(), ITEMS_PER_ROW);
        List<List<Product>> grouped = new ArrayList<>();
        for (int i = 0; i < products.size(); i += ITEMS_PER_ROW) {
            grouped.add(new ArrayList<>(products.subList(i, Math.min(i + ITEMS_PER_ROW, products.size()))));
        }
        log.debug("Total rows created: {}", grouped.size());
        return grouped;
    }

    public Optional<Product> findById(Long id) {
        log.debug("Searching for product with id: {}", id);
        return productRepository.findById(id);
    }
}