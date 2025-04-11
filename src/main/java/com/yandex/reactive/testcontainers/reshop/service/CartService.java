package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Cart;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.domain.enums.CartAction;
import com.yandex.reactive.testcontainers.reshop.exception.ResourceNotFoundException;
import com.yandex.reactive.testcontainers.reshop.repository.CartRepository;
import com.yandex.reactive.testcontainers.reshop.repository.ProductRepository;
import com.yandex.reactive.testcontainers.reshop.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // single user
    private static final Long DEFAULT_USER_ID = 1L;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /**
     * Получение корзины для пользователя по его ID.
     * Возвращает Mono<Cart>.
     */
    public Mono<Cart> getCartForUser(Long userId) {
        log.debug("Fetching cart for user with id {}", userId);
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Пользователь не найден")))
                .flatMap(user -> {
                    log.debug("User found: {}", user);
                    return cartRepository.findByUserId(user.getId())
                            .switchIfEmpty(Mono.defer(() -> {
                                log.info("No cart found for user with id {}, creating a new one", userId);
                                Cart cart = new Cart();
                                cart.setUserId(user.getId());
                                cart.setProducts(new ArrayList<>());
                                cart.setTotalPrice(0.0);
                                return cartRepository.save(cart);
                            }));
                });
    }

    /**
     * Получение корзины для default пользователя.
     */
    public Mono<Cart> getCart() {
        log.debug("Fetching cart for default user");
        return getCartForUser(DEFAULT_USER_ID);
    }

    /**
     * Изменить содержимое корзины для товара с указанным id.
     * @param productId id продукта,
     * @param action действие: "plus" (добавить один айтем),
     *               "minus" (удалить один айтем) или "delete" (удалить продукт полностью).
     * Возвращает обновленную корзину (Mono<Cart>).
     */
    @Transactional
    public Mono<Cart> modifyItem(Long productId, String action) {
        log.info("Modifying item with id {} using action {}", productId, action);
        return getCart()
                .flatMap(cart ->
                        productRepository.findById(productId)
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Продукт не найден с id " + productId)))
                                .flatMap(product -> {
                                    log.debug("Product found: {}", product);
                                    CartAction cartAction;
                                    try {
                                        cartAction = CartAction.valueOf(action.toUpperCase());
                                    } catch (IllegalArgumentException ex) {
                                        log.error("Unknown action: {}", action);
                                        return Mono.error(new IllegalArgumentException("Unknown action: " + action));
                                    }
                                    List<Product> products = cart.getProducts();
                                    switch (cartAction) {
                                        case PLUS:
                                            products.add(product);
                                            break;
                                        case MINUS:
                                            if (products.contains(product)) {
                                                products.remove(product);
                                            }
                                            break;
                                        case DELETE:
                                            products.removeIf(p -> p.getId().equals(productId));
                                            break;
                                        default:
                                            return Mono.error(new IllegalArgumentException("Unknown action: " + action));
                                    }
                                    double total = products.stream().mapToDouble(Product::getPrice).sum();
                                    cart.setTotalPrice(total);
                                    return cartRepository.save(cart)
                                            .doOnNext(updatedCart -> log.info("Cart updated with new total price: {}", total));
                                })
                );
    }

    /**
     * Calculate the quantity of each product in the cart.
     * returns Mono<Map<Long, Integer>>.
     */
    public Mono<Map<Long, Integer>> getProductCounts() {
        return getCart()
                .map(cart -> {
                    Map<Long, Integer> counts = new HashMap<>();
                    for (Product product : cart.getProducts()) {
                        Long productId = product.getId();
                        counts.put(productId, counts.getOrDefault(productId, 0) + 1);
                    }
                    log.debug("Product counts: {}", counts);
                    return counts;
                });
    }

    /**
     * Очистка корзины: удаление всех продуктов и сброс итоговой цены.
     * Возвращает обновленную корзину (Mono<Cart>).
     */
    @Transactional
    public Mono<Cart> clearCart() {
        log.info("Clearing the cart");
        return getCart()
                .flatMap(cart -> {
                    cart.getProducts().clear();
                    cart.setTotalPrice(0.0);
                    return cartRepository.save(cart)
                            .doOnNext(updatedCart -> log.info("Cart cleared and total price reset"));
                });
    }
}

