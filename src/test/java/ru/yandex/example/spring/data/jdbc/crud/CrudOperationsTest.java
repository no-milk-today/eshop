package ru.yandex.example.spring.data.jdbc.crud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.example.spring.data.jdbc.SpringDataJdbcApplicationTest;
import ru.yandex.example.spring.data.jdbc.entity.Account;
import ru.yandex.example.spring.data.jdbc.repository.AccountRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CrudOperationsTest extends SpringDataJdbcApplicationTest {

    @Autowired
    AccountRepository accountRepository;


    @Test
    void testCreate() {
        var anatoly = accountRepository.save(new Account("Анатолий"));

        assertThat(anatoly)
            .isNotNull()
            .withFailMessage("Созданной записи должен был быть присвоен ID")
            .extracting(Account::getId)
            .isNotNull();
    }

    @Test
    void testDelete() {
        var mariana = accountRepository.save(new Account("Мариана"));
        accountRepository.delete(mariana);

        assertThat(accountRepository.existsById(mariana.getId()))
            .withFailMessage("Удаленная запись не должна быть найдена")
            .isFalse();
    }

    @Test
    void testSaveAll() {
        var accounts = List.of(
            new Account("Анатолий"),
            new Account("Мариана"),
            new Account("Александр")
        );
        final Iterable<Account> accountsFromDb = accountRepository.saveAll(accounts);

        assertThat(accounts)
            .hasSize(3)
            .allMatch(account -> account.getId() != null);
    }
}
