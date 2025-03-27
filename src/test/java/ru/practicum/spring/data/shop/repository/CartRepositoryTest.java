package ru.practicum.spring.data.shop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.spring.data.shop.domain.entity.Cart;
import ru.practicum.spring.data.shop.domain.entity.User;

public class CartRepositoryTest extends AbstractDaoTest {

    @Autowired
    private CartRepository underTest;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCreateCartAndFindByUser() {
        var user = User.builder()
                .username("testuser")
                .password("password")
                .email("testuser@example.com")
                .build();
        var userFromDB = userRepository.save(user);

        var cart = new Cart();
        cart.setUser(userFromDB);
        cart.setProducts(new ArrayList<>());
        cart.setTotalPrice(0.0);

        var cartFromDB = underTest.save(cart);
        assertThat(cartFromDB.getId()).isNotNull();

        Optional<Cart> retrievedCart = underTest.findByUser(userFromDB);
        assertThat(retrievedCart).isPresent();
        assertThat(retrievedCart.get().getId()).isEqualTo(cartFromDB.getId());
    }
}