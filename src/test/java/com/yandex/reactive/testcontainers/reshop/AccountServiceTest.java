package com.yandex.reactive.testcontainers.reshop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // интеграционный тест с поднятием контекста
class AccountServiceTest extends AbstractTestContainerTest{

    @Container // декларируем объект учитываемым тест-контейнером
    @ServiceConnection // автоматически назначаем параметры соединения с контейнером
    static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>("mysql:8.0.28");

    @Autowired
    private AccountService accountService;

    @Test
    @DisplayName("Поиск богатых аккаунтов")
    void testFindRichAccounts() {
        // arrange
        var richAccount = new Account("Лагерта", BigDecimal.valueOf(100000));
        var poorAccount = new Account("Рагнар", BigDecimal.ZERO);

        // act
        var richAccounts = accountService.saveAll(
                        richAccount, poorAccount
                ).thenMany(accountService.findRichAccounts(BigDecimal.TEN))
                .toIterable();
        // `toIterable()` internally subscribes to the publisher and blocks until the stream is fully processed

        // assert
        assertThat(richAccounts)
                .withFailMessage("Мы создавали богатые аккаунты, а результат — пустой")
                .isNotEmpty()
                .withFailMessage("Именно один богатый должен быть найден")
                .hasSize(1)
                .first()
                .withFailMessage("Богатый аккаунт должен принадлежать Лагерте")
                .extracting(Account::getName)
                .isEqualTo(richAccount.getName());
    }

}
