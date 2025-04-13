package com.yandex.reactive.testcontainers.reshop.handler;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.dto.Paging;
import com.yandex.reactive.testcontainers.reshop.dto.ProductDTO;
import com.yandex.reactive.testcontainers.reshop.service.CartService;
import com.yandex.reactive.testcontainers.reshop.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductHandler {

    private final ProductService productService;
    private final CartService cartService;

    public ProductHandler(ProductService productService, CartService cartService) {
        this.productService = productService;
        this.cartService = cartService;
    }

    // GET "/" – redirect to "/main/items"
    public Mono<ServerResponse> index(ServerRequest request) {
        return ServerResponse.temporaryRedirect(URI.create("/main/items")).build();
    }

    // GET "/main/items" – витрина продуктов в реактивном стиле.
    public Mono<ServerResponse> mainItems(ServerRequest request) {
        String search = request.queryParam("search").orElse("");
        String sort = request.queryParam("sort").orElse("NO");
        int pageSize = Integer.parseInt(request.queryParam("pageSize").orElse("10"));
        int pageNumber = Integer.parseInt(request.queryParam("pageNumber").orElse("1"));

        Model model = new ConcurrentModel();

        Mono<List<List<Product>>> groupedMono = productService.groupProducts(search, sort, pageNumber, pageSize);
        Mono<Map<Long, Integer>> countsMono = cartService.getProductCounts();

        return groupedMono.zipWith(countsMono)
                .flatMap(tuple -> {
                    List<List<Product>> groupedItems = tuple.getT1();
                    Map<Long, Integer> counts = tuple.getT2();
                    // Устанавливаем для каждого продукта его count
                    groupedItems.forEach(row ->
                            row.forEach(p -> p.setCount(counts.getOrDefault(p.getId(), 0)))
                    );

                    List<List<ProductDTO>> groupedDTOs = groupedItems.stream()
                            .map(row -> row.stream()
                                    .map(this::convertToDTO)
                                    .collect(Collectors.toList()))
                            .collect(Collectors.toList());
                    model.addAttribute("items", groupedDTOs);
                    model.addAttribute("search", search);
                    model.addAttribute("sort", sort);
                    // Получаем paging информацию, вызывая getProducts ещё раз
                    return productService.getProducts(search, sort, pageNumber, pageSize)
                            .doOnNext(page -> {
                                Paging paging = new Paging(pageNumber, pageSize, page.hasNext(), page.hasPrevious());
                                model.addAttribute("paging", paging);
                            })
                            .thenReturn(model);
                })
                .flatMap(m -> ServerResponse.ok().render("main", m.asMap()));
    }

    /**
     * See: <a href="https://stackoverflow.com/questions/50209230/restcontroller-with-spring-webflux-required-parameter-is-not-present">
     * Handling Required Parameters in Spring WebFlux</a>.
     *
     * This reactive approach uses formData() instead of requestParameter to extract data from the form.
     *
     * POST "/main/items/{id}" – modifies the quantity of the product in the cart.
     * After modification, redirect to "/main/items".
     *
     * @param request the current server request
     * @return a Mono emitting a redirect ServerResponse after modifying the item
     */
    public Mono<ServerResponse> modifyMainItems(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return request.formData()
                .doOnNext(data -> log.debug("FormData: {}", data))
                .flatMap(formData -> {
                    String action = formData.getFirst("action");
                    log.info("action: {}", action);
                    return cartService.modifyItem(id, action)
                            .then(ServerResponse.seeOther(URI.create("/main/items")).build());
                });
    }

    // GET "/items/{id}" – отображение деталей продукта.
    public Mono<ServerResponse> getSingleItem(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        Model model = new ConcurrentModel();
        return productService.findById(id)
                .zipWith(cartService.getProductCounts())
                .flatMap(tuple -> {
                    Product product = tuple.getT1();
                    Map<Long, Integer> counts = tuple.getT2();
                    product.setCount(counts.getOrDefault(product.getId(), 0));
                    ProductDTO dto = convertToDTO(product);
                    model.addAttribute("item", dto);
                    return ServerResponse.ok().render("item", model.asMap());
                });
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

