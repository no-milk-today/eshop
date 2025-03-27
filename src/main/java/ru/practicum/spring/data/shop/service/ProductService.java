package ru.practicum.spring.data.shop.service;


import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    // Количество товаров в одном ряду плитки – можно сделать настраиваемым, здесь возьмем фиксированное значение 3.
    private static final int ITEMS_PER_ROW = 3;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Ищет продукты с учётом поиска, сортировки и пагинации.
     * Параметры:
     *   search - строка поиска по name или description
     *   sort - сортировка: "NO" (без сортировки), "ALPHA" (по имени), "PRICE" (по цене)
     *   pageNumber - номер страницы (1-based)
     *   pageSize - размер страницы
     */
    public Page<Product> getProducts(String search, String sort, int pageNumber, int pageSize) {
        // Определяем сортировку
        Sort sortOrder;
        if ("ALPHA".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("name").ascending();
        } else if ("PRICE".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("price").ascending();
        } else {
            sortOrder = Sort.unsorted();
        }

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sortOrder);

        if (search == null || search.trim().isEmpty()) {
            return productRepository.findAll(pageable);
        } else {
            return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
        }
    }

    /**
     * Разбивает список товаров на группы по ITEMS_PER_ROW для отображения плиткой.
     */
    public List<List<Product>> groupProducts(List<Product> products) {
        List<List<Product>> grouped = new ArrayList<>();
        for (int i = 0; i < products.size(); i += ITEMS_PER_ROW) {
            grouped.add(new ArrayList<>(products.subList(i, Math.min(i + ITEMS_PER_ROW, products.size()))));
        }
        return grouped;
    }
}
