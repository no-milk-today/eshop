package ru.practicum.spring.data.shop.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.spring.data.shop.domain.entity.Cart;
import ru.practicum.spring.data.shop.domain.entity.Product;
import ru.practicum.spring.data.shop.domain.entity.User;
import ru.practicum.spring.data.shop.domain.enums.CartAction;
import ru.practicum.spring.data.shop.repository.CartRepository;
import ru.practicum.spring.data.shop.repository.ProductRepository;
import ru.practicum.spring.data.shop.repository.UserRepository;

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

    public Cart getCartForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Optional<Cart> optionalCart = cartRepository.findByUser(user);
        if (optionalCart.isPresent()) {
            return optionalCart.get();
        } else {
            Cart cart = new Cart();
            cart.setUser(user);
            cart.setProducts(new java.util.ArrayList<>());
            cart.setTotalPrice(0.0);
            return cartRepository.save(cart);
        }
    }

    /**
     * Get cart for the default single user.
     */
    public Cart getCart() {
        return getCartForUser(DEFAULT_USER_ID);
    }

    /**
     * Изменить содержимое корзины для товара с указанным id.
     * @param productId идентификатор продукта,
     * @param action действие из перечисления: "plus" (добавить один айтем),
     *               "minus" (удалить один айтем) или "delete" (удалить продактс полностью).
     */
    @Transactional
    public void modifyItem(Long productId, String action) {
        Cart cart = getCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Продукт не найден с id " + productId));

        // Convert enum
        CartAction cartAction;
        try {
            cartAction = CartAction.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown action: " + action);
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
                throw new IllegalArgumentException("Unknown action: " + action);
        }
        double total = products.stream().mapToDouble(Product::getPrice).sum();
        cart.setTotalPrice(total);
        cartRepository.save(cart);
    }

    /**
     * Calculate the quantity of each product in the cart.
     */
    public Map<Long, Integer> getProductCounts() {
        Cart cart = getCart();
        Map<Long, Integer> counts = new HashMap<>();
        for (Product product : cart.getProducts()) {
            Long productId = product.getId();
            counts.put(productId, counts.getOrDefault(productId, 0) + 1);
        }
        return counts;
    }

    /**
     * Clear the cart by removing all products and resetting the total price.
     */
    @Transactional
    public void clearCart() {
        Cart cart = getCart();
        cart.getProducts().clear();
        cart.setTotalPrice(0.0);
        cartRepository.save(cart);
    }
}