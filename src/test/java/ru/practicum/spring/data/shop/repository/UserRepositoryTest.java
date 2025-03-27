package ru.practicum.spring.data.shop.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.spring.data.shop.domain.entity.User;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class UserRepositoryTest extends AbstractDaoTest {

    @Autowired
    private UserRepository underTest;

    @Test
    void testCreateUser() {
        var user = User.builder()
                .username("testuser")
                .password("password")
                .email("testuser@example.com")
                .build();

        var savedUser = underTest.save(user);
        assertThat(savedUser.getId()).isNotNull();
    }

    @Test
    void testFindById() {
        var user = User.builder()
                .username("finduser")
                .password("password")
                .email("finduser@example.com")
                .build();

        var savedUser = underTest.save(user);

        Optional<User> retrievedUser = underTest.findById(savedUser.getId());
        assertThat(retrievedUser).isPresent();
    }
}
