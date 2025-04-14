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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartProductRepository cartProductRepository;

    // single user
    private static final Long DEFAULT_USER_ID = 1L;

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
     * Получение корзины для default пользователя.
     */
    public Mono<Cart> getCart() {
        log.debug("Fetching cart for default user");
        return getCartForUser(DEFAULT_USER_ID);
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

//    В методе modifyItem осуществляется добавление (PLUS), удаление одного (MINUS) или всех (DELETE) записей из join‑таблицы через CartProductRepository.
//    После изменений вызывается метод updateCartTotal, который агрегирует цену всех продуктов (считывая данные через join‑таблицу) и обновляет поле totalPrice в корзине.
}

