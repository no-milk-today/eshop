package com.yandex.reactive.testcontainers.reshop.handler;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.dto.CartItemDto;
import com.yandex.reactive.testcontainers.reshop.service.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CartHandler {

    private final CartService cartService;

    public CartHandler(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * GET "/cart/items" – отображение корзины.
     * Получает корзину и count товаров, затем преобразует товары в CartItemDto и рендерит темплейт.
     */
    public Mono<ServerResponse> getCartItems(ServerRequest request) {
        Model model = new ConcurrentModel();

        return cartService.getCart()
                .flatMap(cart ->
                        cartService.getProductCounts().map(counts -> {
                            // Получаем уникальные товары по id (сохраняем первый)
                            var uniqueProducts = cart.getProducts().stream()
                                    .collect(Collectors.toMap(
                                            Product::getId,
                                            p -> p,
                                            (p1, p2) -> p1
                                    ))
                                    .values();

                            List<CartItemDto> dtos = uniqueProducts.stream()
                                    .map(p -> CartItemDto.builder()
                                            .id(p.getId())
                                            .name(p.getName())
                                            .description(p.getDescription())
                                            .imgPath(p.getImgPath())
                                            .price(p.getPrice())
                                            .count(counts.getOrDefault(p.getId(), 0))
                                            .build())
                                    .collect(Collectors.toList());

                            model.addAttribute("products", dtos);
                            model.addAttribute("total", cart.getTotalPrice());
                            model.addAttribute("empty", cart.getProducts().isEmpty());
                            return model;
                        })
                )
                .flatMap(m -> ServerResponse.ok().render("cart", m.asMap()));
    }

    /**
     * POST "/cart/items/{id}" – изменение количества товара в корзине.
     * После модификации редирект на "/cart/items".
     */
    public Mono<ServerResponse> modifyCartItem(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return request.formData()
                .flatMap(formData -> {
                    String action = formData.getFirst("action");
                    return cartService.modifyItem(id, action)
                            .then(ServerResponse.temporaryRedirect(URI.create("/cart/items")).build());
                });
    }
}
