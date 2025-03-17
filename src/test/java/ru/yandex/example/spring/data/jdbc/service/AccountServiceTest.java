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

    @Test
    void testSuccessfulTransfer() {
        accountDao.create("Petr");
        accountDao.create("Vasily");
        var petrAccount = accountDao.findFirstByName("Petr");
        var vasilyAccount = accountDao.findFirstByName("Vasily");
        BigDecimal amount = BigDecimal.valueOf(500);
        BigDecimal initialPetrBalance = petrAccount.getBalance();
        BigDecimal initialVasilyBalance = vasilyAccount.getBalance();

        accountService.transfer(petrAccount, vasilyAccount, amount);

        var updatedPetrAccount = accountDao.findFirstByName("Petr");
        var updatedVasilyAccount = accountDao.findFirstByName("Vasily");

        // Check that Petr's balance decreased and Vasily's increased
        assertThat(updatedPetrAccount.getBalance())
                .isEqualByComparingTo(initialPetrBalance.subtract(amount));
        assertThat(updatedVasilyAccount.getBalance())
                .isEqualByComparingTo(initialVasilyBalance.add(amount));
    }

    @Test
    void testMultipleTransfers() {
        accountDao.create("Alice");
        accountDao.create("Bob");
        var aliceAccount = accountDao.findFirstByName("Alice");
        var bobAccount = accountDao.findFirstByName("Bob");
        BigDecimal initialAliceBalance = aliceAccount.getBalance();
        BigDecimal initialBobBalance = bobAccount.getBalance();

        // Transfer 1000 from Alice to Bob
        accountService.transfer(aliceAccount, bobAccount, BigDecimal.valueOf(1000));
        // Refresh accounts
        aliceAccount = accountDao.findFirstByName("Alice");
        bobAccount = accountDao.findFirstByName("Bob");

        // Transfer 2000 from Alice to Bob
        accountService.transfer(aliceAccount, bobAccount, BigDecimal.valueOf(2000));
        // Refresh accounts
        aliceAccount = accountDao.findFirstByName("Alice");
        bobAccount = accountDao.findFirstByName("Bob");

        BigDecimal expectedAliceBalance = initialAliceBalance.subtract(BigDecimal.valueOf(3000));
        BigDecimal expectedBobBalance = initialBobBalance.add(BigDecimal.valueOf(3000));

        assertThat(aliceAccount.getBalance())
                .isEqualByComparingTo(expectedAliceBalance);
        assertThat(bobAccount.getBalance())
                .isEqualByComparingTo(expectedBobBalance);
    }
}