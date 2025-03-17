package ru.yandex.example.spring.data.jdbc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;
import ru.yandex.example.spring.data.jdbc.SpringDataJdbcApplicationTest;
import ru.yandex.example.spring.data.jdbc.entity.Account;
import ru.yandex.example.spring.data.jdbc.repository.AccountDao;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountServiceTest extends SpringDataJdbcApplicationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountDao accountDao;

    @BeforeEach
    void cleanTables() {
        accountDao.deleteAll();
    }

    @Test
    void testSuccessfulSqlQueries() {
        // Инициализируем пользователей (изначальный баланс — 10000)
        accountDao.create("Пётр");
        accountDao.create("Василий");
        var petrAccount = accountDao.findFirstByName("Пётр");
        var vasilyAccount = accountDao.findFirstByName("Василий");
        var initialBalance = petrAccount.getBalance();

        // Переводим от Василия Петру 100000 (возникает ошибка ограничения на баланс)
        assertThrows(
                UncategorizedSQLException.class,
                () -> accountService.transfer(vasilyAccount, petrAccount, BigDecimal.valueOf(100_000L))
        );

        // Проверяем, что транзакция откатилась
        // Не должно возникнуть ситуации, что Петру деньги начислились, а с Василия не списались
        assertThat(accountDao.findAll())
                .isNotEmpty()
                .withFailMessage("При возникновении ошибки во время транзакции " +
                        "балансы обоих пользователей должны вернуться к изначальным значениям")
                .map(Account::getBalance)
                .allMatch(it -> it.compareTo(initialBalance) == 0);
    }

    @Test
    void testSaveAll() {
        var accounts = List.of(
                new Account("Анатолий"),
                new Account("Мариана"),
                new Account("Александр")
        );
        accountDao.saveAll(accounts);

        assertThat(accountDao.findAll())
                .isNotEmpty()
                .hasSize(3);
    }
}
