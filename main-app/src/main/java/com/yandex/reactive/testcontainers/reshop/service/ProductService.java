package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.domain.enums.ProductSort;
import com.yandex.reactive.testcontainers.reshop.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

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
     * Реализована сортировка с помощью оператора Flux.sort() и пагинация через skip() и take().
     *
     * @param search     search string for name or description
     * @param sort       "NO" (no sorting), "ALPHA" (by name), "PRICE" (by price)
     * @param pageNumber page number (1-based)
     * @param pageSize   page size
     * @return Flux<Product> – поток найденных продуктов
     */
    public Flux<Product> getProducts(String search, String sort, int pageNumber, int pageSize) {
        log.debug("Fetching products with search: '{}', sort: '{}', pageNumber: {}, pageSize: {}",
                search, sort, pageNumber, pageSize);

        // Определяем тип сортировки по строке
        ProductSort sortType = ProductSort.from(sort);
        Comparator<Product> comparator = null;
        switch (sortType) {
            case ALPHA:
                comparator = Comparator.comparing(Product::getName);
                break;
            case PRICE:
                comparator = Comparator.comparingDouble(Product::getPrice);
                break;
            default:
                // NO сортировки
                break;
        }

        Flux<Product> productFlux;
        if (search == null || search.trim().isEmpty()) {
            // Если поиск не указан, получаем все продукты
            productFlux = productRepository.findAll();
        } else {
            productFlux = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search);
        }
        // Если компаратор не null, выполняем сортировку
        if (comparator != null) {
            productFlux = productFlux.sort(comparator);
        }
        // Реализуем пагинацию через skip и take.
        return productFlux
                .skip((pageNumber - 1L) * pageSize)
                .take(pageSize);
    }

    /**
     * Splits the list of products into groups of ITEMS_PER_ROW for tile display.
     *
     * @param productsFlux Flux of products
     * @return Mono<List<List<Product>>> – список рядов, где каждый ряд — это список товаров
     */
    public Mono<List<List<Product>>> groupProducts(Flux<Product> productsFlux) {
        return productsFlux
                .collectList()
                .map(products -> {
                    log.debug("Grouping {} products into rows of {} items each", products.size(), ITEMS_PER_ROW);
                    List<List<Product>> grouped = new java.util.ArrayList<>();
                    for (int i = 0; i < products.size(); i += ITEMS_PER_ROW) {
                        grouped.add(new java.util.ArrayList<>(products.subList(i, Math.min(i + ITEMS_PER_ROW, products.size()))));
                    }
                    log.debug("Total rows created: {}", grouped.size());
                    return grouped;
                });
    }

    public Mono<Product> findById(Long id) {
        log.debug("Searching for product with id: {}", id);
        return productRepository.findById(id);
    }
}
