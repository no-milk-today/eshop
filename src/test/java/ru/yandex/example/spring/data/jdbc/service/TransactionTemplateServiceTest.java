package ru.yandex.example.spring.data.jdbc.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import ru.yandex.example.spring.data.jdbc.SpringDataJdbcApplicationTest;
import ru.yandex.example.spring.data.jdbc.entity.Account;
import ru.yandex.example.spring.data.jdbc.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class TransactionTemplateServiceTest extends SpringDataJdbcApplicationTest {

    @Autowired
    TransactionTemplateService transactionManagerService;

    @Autowired
    AccountRepository accountRepository;

    @Test
    void testTransactionManagerValid() {
        // Инициализируем пользователей (изначальный баланс — 10000)
        var petr = accountRepository.save(new Account("Пётр"));
        var vasily = accountRepository.save(new Account("Василий"));
        var initialBalance = petr.getBalance();

        // Переводим от Василия Петру 100000 (возникает ошибка ограничения на баланс)
        Assertions.assertThrows(
                DbActionExecutionException.class,
                () -> transactionManagerService.transfer(vasily, petr, BigDecimal.valueOf(100_000L))
        );

        // Проверяем, что транзакция откатилась
        // Не должно возникнуть ситуации, что Петру деньги начислились, а с Василия не списались
        assertThat(accountRepository.findAllById(List.of(petr.getId(), vasily.getId())))
                .isNotEmpty()
                .withFailMessage("При возникновении ошибки во время транзакции " +
                        "балансы обоих пользователей должны вернуться к изначальным значениям")
                .map(Account::getBalance)
                .allMatch(it -> it.compareTo(initialBalance) == 0);
    }

}
