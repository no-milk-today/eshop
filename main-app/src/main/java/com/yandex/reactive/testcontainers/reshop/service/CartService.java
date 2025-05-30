package com.yandex.reactive.testcontainers.reshop.service;

import com.yandex.reactive.testcontainers.reshop.domain.entity.Cart;
import com.yandex.reactive.testcontainers.reshop.domain.entity.CartProduct;
import com.yandex.reactive.testcontainers.reshop.domain.entity.Product;
import com.yandex.reactive.testcontainers.reshop.domain.enums.CartAction;
import com.yandex.reactive.testcontainers.reshop.exception.ResourceNotFoundException;
import com.yandex.reactive.testcontainers.reshop.repository.CartProductRepository;
import com.yandex.reactive.testcontainers.reshop.repository.CartRepository;
import com.yandex.reactive.testcontainers.reshop.repository.ProductRepository;
import com.yandex.reactive.testcontainers.reshop.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartProductRepository cartProductRepository;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository,
                       CartProductRepository cartProductRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartProductRepository = cartProductRepository;
    }

    /**
     * Получение корзины для пользователя по его ID.
     * Возвращает Mono<Cart>.
     */
    public Mono<Cart> getCartForUser(Long userId) {
        log.debug("Fetching cart for user with id {}", userId);
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Пользователь не найден")))
                .flatMap(user ->
                        cartRepository.findByUserId(user.getId())
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.info("No cart found for user with id {}, creating a new one", userId);
                                    Cart cart = new Cart();
                                    cart.setUserId(user.getId());
                                    // Поле products теперь можно не заполнять, так как данные хранятся в join‑таблице
                                    cart.setTotalPrice(0.0);
                                    return cartRepository.save(cart);
                                }))
                );
    }

    /**
     * Получение корзины для currently authenticated пользователя.
     * If the authentication principal is an OIDC user, the preferred username is used for lookup.
     *
     * @return a Mono with existing or brand-new Cart
     * @throws ResourceNotFoundException if the user is not authenticated or not found in the DB
     */
    public Mono<Cart> getCart() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .doOnNext(auth -> log.debug("Authentication found: {}", auth.getName()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not authenticated")))
                .flatMap(auth -> {
                    String username;
                    if (auth.getPrincipal() instanceof OidcUser oidcUser) {
                        username = oidcUser.getPreferredUsername();
                    } else {
                        username = auth.getName();
                    }
                    return userRepository.findByUsername(username)
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found in DB")));
                })
                .flatMap(user ->
                        cartRepository.findByUserId(user.getId())
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.info("No cart found for user with id {}, creating a new one", user.getId());
                                    Cart newCart = new Cart();
                                    newCart.setUserId(user.getId());
                                    newCart.setTotalPrice(0.0);
                                    return cartRepository.save(newCart);
                                }))
                );
    }

    /**
     * Вспомогательный метод для пересчета итоговой суммы корзины.
     *
     * @param cart корзина
     * @return Mono<Cart> с обновленным totalPrice
     */
    private Mono<Cart> updateCartTotal(Cart cart) {
        return cartProductRepository.findByCartId(cart.getId())
                .flatMap(cartProduct ->
                        productRepository.findById(cartProduct.getProductId())
                )
                .map(Product::getPrice)
                .reduce(0.0, Double::sum)
                .flatMap(total -> {
                    cart.setTotalPrice(total);
                    return cartRepository.save(cart);
                });
    }

    /**
     * Модифицирует содержимое корзины для товара с указанным id.
     * В зависимости от действия (plus, minus, delete) добавляет или удаляет элемент.
     *
     * @param productId идентификатор продукта
     * @param action    действие: "plus", "minus", "delete"
     * @return Mono<Void>
     */
    public Mono<Void> modifyItem(Long productId, String action) {
        log.info("Modifying item with id {} using action {}", productId, action);
        return Mono.zip(getCart(), productRepository.findById(productId))
                .flatMap(tuple -> {
                    Cart cart = tuple.getT1();
                    Product product = tuple.getT2();

                    CartAction cartAction;
                    try {
                        cartAction = CartAction.valueOf(action.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        log.error("Unknown action: {}", action);
                        return Mono.error(new IllegalArgumentException("Unknown action: " + action));
                    }

                    switch (cartAction) {
                        case PLUS:
                            CartProduct cp = new CartProduct();
                            cp.setCartId(cart.getId());
                            cp.setProductId(productId);
                            return cartProductRepository.save(cp)
                                    .then(updateCartTotal(cart));
                        case MINUS:
                            // Удаляем первый найденный экземпляр
                            return cartProductRepository.findFirstByCartIdAndProductId(cart.getId(), productId)
                                    .flatMap(cpToRemove -> cartProductRepository.deleteById(cpToRemove.getId()))
                                    .then(updateCartTotal(cart));
                        case DELETE:
                            // Удаляем все вхождения продукта из корзины
                            return cartProductRepository.findAllByCartIdAndProductId(cart.getId(), productId)
                                    .flatMap(cpToRemove -> cartProductRepository.deleteById(cpToRemove.getId()))
                                    .then(updateCartTotal(cart));
                        default:
                            return Mono.error(new IllegalArgumentException("Unknown action: " + action));
                    }
                })
                .doOnSuccess(updatedCart ->
                        log.info("Cart updated with new total price: {}", updatedCart.getTotalPrice()))
                .then();
    }


    /**
     * Очищает корзину, удаляя все записи из join‑таблицы и сбрасывая итоговую сумму.
     */
    public Mono<Void> clearCart() {
        log.info("Clearing the cart");
        return getCart()
                .flatMap(cart ->
                        cartProductRepository.findByCartId(cart.getId())
                                .flatMap(cp -> cartProductRepository.deleteById(cp.getId()))
                                .then(Mono.defer(() -> {
                                    cart.setTotalPrice(0.0);
                                    return cartRepository.save(cart);
                                }))
                )
                .doOnSuccess(updatedCart ->
                        log.info("Cart cleared and total price reset"))
                .then();
    }

}

