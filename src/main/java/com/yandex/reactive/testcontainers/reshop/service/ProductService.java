package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.domain.enums.ProductSort;
import com.yandex.reactive.testcontainers.reshop.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
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
     * REACTIVE method for searching products with support for search string, sorting, and pagination.
     * Returns a Mono wrapping a Page<Product>.
     */
    public Mono<Page<Product>> getProducts(String search, String sort, int pageNumber, int pageSize) {
        log.debug("Fetching products with search: '{}', sort: '{}', pageNumber: {}, pageSize: {}",
                search, sort, pageNumber, pageSize);

        ProductSort sortType = ProductSort.from(sort);
        Sort sortOrder;
        if (sortType == ProductSort.ALPHA) {
            sortOrder = Sort.by("name").ascending();
        } else if (sortType == ProductSort.PRICE) {
            sortOrder = Sort.by("price").ascending();
        } else {
            sortOrder = Sort.unsorted();
        }

        // нет поддержки Pageable в реактив репо, поэтому создаем его вручную
        int offset = (pageNumber - 1) * pageSize;
        Mono<Long> totalMono;
        Flux<Product> productFlux;

        if (search == null || search.trim().isEmpty()) {
            productFlux = productRepository.findAllProducts(pageSize, offset);
            totalMono = productRepository.countProducts();
        } else {
            productFlux = productRepository.searchProducts(search, pageSize, offset);
            totalMono = productRepository.countProductsBySearch(search);
        }
        return productFlux.collectList()
                .zipWith(totalMono)
                .map(tuple -> new PageImpl<>(tuple.getT1(), PageRequest.of(pageNumber - 1, pageSize, sortOrder), tuple.getT2()));
    }

    /**
     * Splits the list of products into groups of ITEMS_PER_ROW for display in a grid.
     * This method first retrieves a page of products (as Mono<Page<Product>>),
     * and then groups the page content.
     */
    public Mono<List<List<Product>>> groupProducts(String search, String sort, int pageNumber, int pageSize) {
        return getProducts(search, sort, pageNumber, pageSize)
                .map(page -> {
                    List<Product> products = page.getContent();
                    List<List<Product>> grouped = new ArrayList<>();
                    for (int i = 0; i < products.size(); i += ITEMS_PER_ROW) {
                        grouped.add(new ArrayList<>(products.subList(i, Math.min(i + ITEMS_PER_ROW, products.size()))));
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
