package com.yandex.reactive.testcontainers.testconainers_demo_raactive.repository;


import com.yandex.reactive.testcontainers.testconainers_demo_raactive.AbstractDaoTest;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.Order;
import com.yandex.reactive.testcontainers.testconainers_demo_raactive.domain.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderRepositoryTest extends AbstractDaoTest {

    @Autowired
    private OrderRepository underTest;

    @Autowired
    private UserRepository userRepository;

@Test
void testCreateOrderAndFindByNumber() {
    // Create a user
    var user = User.builder()
            .username("orderuser")
            .password("password")
            .email("orderuser@example.com")
            .build();

    // Save the user and create an order
    userRepository.save(user)
            .flatMap(userFromDB -> {
                // Create an order with the saved user's ID
                var order = new Order();
                order.setUserId(userFromDB.getId()); // Save only the user ID
                order.setProducts(new ArrayList<>()); // Products are transient
                order.setOrderDate(LocalDateTime.now());
                order.setNumber("ORD123");
                order.setTotalSum(99.99);

                    return underTest.save(order)
                            .doOnNext(orderFromDB ->
                                    assertThat(orderFromDB.getId())
                                            .withFailMessage("Saved order should have an ID assigned")
                                            .isNotNull()
                            )
                            // После сохранения ищем заказы по номеру
                            .flatMap(orderFromDB ->
                                    underTest.findByNumber("ORD123")
                                            .collectList()
                                            .doOnNext(orders -> {
                                                assertThat(orders)
                                                        .withFailMessage("Orders with the given number should not be empty")
                                                        .isNotEmpty();
                                                assertThat(orders.get(0).getId())
                                                        .withFailMessage("Retrieved order should match saved order")
                                                        .isEqualTo(orderFromDB.getId());
                                            })
                                            .thenReturn(orderFromDB)
                            );
                })
                .block(); // Блокировка до завершения всей цепочки
    }
}
