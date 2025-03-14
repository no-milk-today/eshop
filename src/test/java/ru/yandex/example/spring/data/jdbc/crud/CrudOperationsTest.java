package ru.yandex.example.spring.data.jdbc.crud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.example.spring.data.jdbc.SpringDataJdbcApplicationTest;
import ru.yandex.example.spring.data.jdbc.entity.Account;
import ru.yandex.example.spring.data.jdbc.repository.AccountRepository;

import static org.assertj.core.api.Assertions.assertThat;

class CrudOperationsTest extends SpringDataJdbcApplicationTest {

    @Autowired
    AccountRepository accountRepository;

    @Test
    public void testCreate() {
        var anatoly = accountRepository.save(new Account("Анатолий"));

        assertThat(anatoly)
            .isNotNull()
            .withFailMessage("Созданной записи должен был быть присвоен ID")
            .extracting(Account::getId)
            .isNotNull();
    }

    @Test
    public void testDelete() {
        var mariana = accountRepository.save(new Account("Мариана"));
        accountRepository.delete(mariana);

        assertThat(accountRepository.existsById(mariana.getId()))
            .withFailMessage("Удаленная запись не должна быть найдена")
            .isFalse();
    }
}
