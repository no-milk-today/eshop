package com.yandex.reactive.testcontainers.testconainers_demo_raactive.repository;

import com.yandex.reactive.testcontainers.testconainers_demo_raactive.AbstractDaoTest;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.Cart;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.User;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
@Disabled
public class CartRepositoryTest extends AbstractDaoTest {

    @Autowired
    private CartRepository underTest;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCreateCartAndFindByUser() {
        // Создаем пользователя
        var user = User.builder()
                .username("testuser")
                .password("password")
                .email("testuser@example.com")
                .build();

        // Реактивная цепочка: сохраняем пользователя, затем корзину и затем ищем корзину по пользователю.
        userRepository.save(user)
                .flatMap(userFromDB -> {
                    // Создаем корзину с сохраненным пользователем
                    var cart = new Cart();
                    cart.setUserId(userFromDB.getId());
                    cart.setProducts(new ArrayList<>());  // для реактивного подхода ManyToMany будем подгружать данные отдельно
                    cart.setTotalPrice(0.0);

                    return underTest.save(cart)
                            .doOnNext(cartFromDB ->
                                    assertThat(cartFromDB.getId())
                                            .withFailMessage("Saved cart should have an ID assigned")
                                            .isNotNull()
                            )
                            .flatMap(savedCart ->
                                    underTest.findByUserId(userFromDB.getId())
                                            .doOnNext(foundCart ->
                                                    assertThat(foundCart.getId())
                                                            .withFailMessage("Retrieved cart should match saved cart")
                                                            .isEqualTo(savedCart.getId())
                                            )
                            );
                })
                .block(); // Блокируем реактивную цепочку до завершения
    }
}
