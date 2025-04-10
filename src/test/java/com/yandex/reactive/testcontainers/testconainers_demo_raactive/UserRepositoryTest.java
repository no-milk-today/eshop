package com.yandex.reactive.testcontainers.testconainers_demo_raactive;

import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.User;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.repository.UserRepository;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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

        underTest.save(user)
                .doOnNext(userFromDB ->
                        AssertionsForClassTypes.assertThat(userFromDB)
                                .withFailMessage("Result of save should not be null")
                                .isNotNull()
                                .extracting(User::getId)
                                .withFailMessage("Saved user should have an ID assigned")
                                .isNotNull()
                )
                .block();
    }

    @Test
    void testFindById() {
        var user = User.builder()
                .username("finduser")
                .password("password")
                .email("finduser@example.com")
                .build();

        underTest.save(user)
                .flatMap(userFromDB ->
                        // После сохранения ищем юзера по ID и проверяем его
                        underTest.findById(userFromDB.getId())
                                .doOnNext(foundUser -> {
                                    AssertionsForClassTypes.assertThat(foundUser)
                                            .withFailMessage("Found user should not be null")
                                            .isNotNull();
                                    AssertionsForClassTypes.assertThat(foundUser.getUsername())
                                            .withFailMessage("User name should match")
                                            .isEqualTo("finduser");
                                })
                )
                .block();
    }
}
