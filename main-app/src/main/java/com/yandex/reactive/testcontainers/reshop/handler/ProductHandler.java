package com.yandex.reactive.testcontainers.reshop.handler;


import com.yandex.reactive.testcontainers.reshop.domain.entity.CartProduct;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.dto.Paging;
import com.yandex.reactive.testcontainers.reshop.dto.ProductDTO;
import com.yandex.reactive.testcontainers.reshop.exception.ResourceNotFoundException;
import com.yandex.reactive.testcontainers.reshop.repository.CartProductRepository;
import com.yandex.reactive.testcontainers.reshop.service.CartService;
import com.yandex.reactive.testcontainers.reshop.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductHandler {

    private final ProductService productService;
    private final CartService cartService;
    private final CartProductRepository cartProductRepository;

    public ProductHandler(ProductService productService,
                          CartService cartService,
                          CartProductRepository cartProductRepository) {
        this.productService = productService;
        this.cartService = cartService;
        this.cartProductRepository = cartProductRepository;
    }

    // GET "/" – redirect to "/main/items"
    public Mono<ServerResponse> index(ServerRequest request) {
        return ServerResponse.temporaryRedirect(URI.create("/main/items")).build();
    }

    /**
     * GET "/main/items" – витрина продуктов.
     * Извлекает query-параметры, выполняет поиск, сортировку и пагинацию.
     * Затем группирует товары (например, по 3 в ряд) и формирует список DTO для отображения.
     * Количество для каждого продукта берётся через локальный метод getProductCounts().
     */
    public Mono<ServerResponse> mainItems(ServerRequest request) {
        String search = request.queryParam("search").orElse("");
        String sort = request.queryParam("sort").orElse("NO");
        int pageSize = Integer.parseInt(request.queryParam("pageSize").orElse("10"));
        int pageNumber = Integer.parseInt(request.queryParam("pageNumber").orElse("1"));

        Flux<Product> productFlux = productService.getProducts(search, sort, pageNumber, pageSize);
        Mono<List<List<Product>>> groupedMono = productService.groupProducts(productFlux);
        Mono<Map<Long, Integer>> countsMono = getProductCounts();

        return Mono.zip(groupedMono, countsMono)
                .flatMap(tuple -> {
                    List<List<Product>> groupedProducts = tuple.getT1();
                    Map<Long, Integer> counts = tuple.getT2();

                    // Для каждого продукта устанавливаем поле count из полученных данных (если не найден – 0)
                    groupedProducts.forEach(row ->
                            row.forEach(p -> p.setCount(counts.getOrDefault(p.getId(), 0)))
                    );

                    // Конвертация в DTO для отображения
                    List<List<ProductDTO>> groupedDTOs = groupedProducts.stream()
                            .map(row -> row.stream()
                                    .map(this::convertToDTO)
                                    .collect(Collectors.toList())
                            )
                            .collect(Collectors.toList());

                    // если выдано ровно pageSize товаров, то есть следующая страница
                    int displayedCount = groupedProducts.stream().mapToInt(List::size).sum();
                    boolean hasNext = displayedCount == pageSize;
                    boolean hasPrevious = pageNumber > 1;
                    Paging paging = new Paging(pageNumber, pageSize, hasNext, hasPrevious);

                    Map<String, Object> model = new HashMap<>();
                    model.put("items", groupedDTOs);
                    model.put("search", search);
                    model.put("sort", sort);
                    model.put("paging", paging);

                    return ServerResponse.ok().render("main", model);
                });
    }

    /**
     * POST "/main/items/{id}" – изменение количества товара в корзине.
     * Извлекает из formData параметр action и вызывает метод изменения в CartService.
     * После выполнения выполняется редирект на "/main/items".
     */
    public Mono<ServerResponse> modifyMainItems(ServerRequest request) {
        String idStr = request.pathVariable("id");
        Long productId;
        try {
            productId = Long.valueOf(idStr);
        } catch (NumberFormatException e) {
            return ServerResponse.badRequest().build();
        }
        return request.formData().flatMap(formData -> {
            String action = formData.getFirst("action");
            if (action == null || action.isEmpty()) {
                return ServerResponse.badRequest().build();
            }
            return cartService.modifyItem(productId, action)
                    .then(ServerResponse.seeOther(URI.create("/main/items")).build());
        });
    }

    /**
     * GET "/items/{id}" – отображение деталей продукта.
     * Получает продукт по id, обновляет его значение count, конвертирует в DTO и рендерит шаблон "item".
     */
    public Mono<ServerResponse> getSingleItem(ServerRequest request) {
        String idStr = request.pathVariable("id");
        Long productId;
        try {
            productId = Long.valueOf(idStr);
        } catch (NumberFormatException e) {
            return ServerResponse.badRequest().build();
        }
        return productService.findById(productId)
                .doOnError(e -> log.error("Error in getSingleItem", e))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Продукт с id " + productId + " не найден")))
                .flatMap(product ->
                        getProductCounts().map(counts -> {
                            product.setCount(counts.getOrDefault(product.getId(), 0));
                            return product;
                        })
                )
                .flatMap(product -> {
                    ProductDTO dto = convertToDTO(product);
                    Map<String, Object> model = new HashMap<>();
                    model.put("item", dto);
                    return ServerResponse.ok().render("item", model);
                });
    }

    /**
     * Вспомогательный метод для получения количества каждого продукта для текущей корзины.
     * Группировка происходит по записям из join‑таблицы (CartProductRepository).
     */
    private Mono<Map<Long, Integer>> getProductCounts() {
        return cartService.getCart()
                .flatMapMany(cart ->
                        cartProductRepository.findByCartId(cart.getId())
                )
                .collect(Collectors.groupingBy(
                        CartProduct::getProductId,
                        Collectors.summingInt(cp -> 1)
                ));
    }

    private ProductDTO convertToDTO(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImgPath(),
                product.getCount()
        );
    }
}

