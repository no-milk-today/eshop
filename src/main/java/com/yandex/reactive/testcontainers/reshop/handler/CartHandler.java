package com.yandex.reactive.testcontainers.reshop.handler;

import com.yandex.reactive.testcontainers.reshop.domain.entity.CartProduct;
import com.yandex.reactive.testcontainers.reshop.dto.CartItemDto;
import com.yandex.reactive.testcontainers.reshop.repository.CartProductRepository;
import com.yandex.reactive.testcontainers.reshop.repository.ProductRepository;
import com.yandex.reactive.testcontainers.reshop.service.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CartHandler {

    private final CartService cartService;
    private final CartProductRepository cartProductRepository;
    private final ProductRepository productRepository;

    public CartHandler(CartService cartService,
                       CartProductRepository cartProductRepository,
                       ProductRepository productRepository) {
        this.cartService = cartService;
        this.cartProductRepository = cartProductRepository;
        this.productRepository = productRepository;
    }

    /**
     * GET "/cart/items" – отображение корзины.
     * Получает корзину, затем через join‑таблицу формирует список уникальных товаров с посчитанным количеством.
     * Рендерит шаблон "cart.html" с параметрами:
     * - products: список CartItemDto,
     * - total: общая сумма корзины,
     * - empty: признак, что корзина пуста.
     */
    public Mono<ServerResponse> getCartItems(ServerRequest request) {
        return cartService.getCart().flatMap(cart ->
                cartProductRepository.findByCartId(cart.getId())
                        .collectList()
                        .flatMap(cartProducts -> {
                            // Группируем записи из join‑таблицы по productId с подсчетом количества
                            Map<Long, Long> productCounts = cartProducts.stream()
                                    .collect(Collectors.groupingBy(
                                            CartProduct::getProductId,
                                            Collectors.counting()
                                    ));
                            Set<Long> productIds = productCounts.keySet();
                            // Получаем список уникальных продуктов
                            return Flux.fromIterable(productIds)
                                    .flatMap(productRepository::findById)
                                    .collectList()
                                    .flatMap(products -> {
                                        List<CartItemDto> cartItemDtos = products.stream()
                                                .map(product -> CartItemDto.builder()
                                                        .id(product.getId())
                                                        .name(product.getName())
                                                        .description(product.getDescription())
                                                        .imgPath(product.getImgPath())
                                                        .price(product.getPrice())
                                                        .count(productCounts.get(product.getId()).intValue())
                                                        .build())
                                                .collect(Collectors.toList());
                                        Map<String, Object> model = new HashMap<>();
                                        model.put("products", cartItemDtos);
                                        model.put("total", cart.getTotalPrice());
                                        model.put("empty", cartProducts.isEmpty()); // to be removed
                                        return ServerResponse.ok().render("cart", model);
                                    });
                        })
        ).switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                .render("not-found", Map.of("errorMessage", "Корзина не найдена")));
    }

    /**
     * POST "/cart/items/{id}"
     * Извлекает из path переменную productId и action из formData.
     * Вызывает метод изменения содержимого корзины и затем делает редирект на "/cart/items".
     */
    public Mono<ServerResponse> modifyCartItem(ServerRequest request) {
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
                    .then(ServerResponse.seeOther(URI.create("/cart/items")).build());
        });
    }
}
