package ru.yandex.example.spring.data.jdbc.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.example.spring.data.jdbc.entity.Account;

@Repository
public interface AccountRepository extends CrudRepository<Account, Integer> {
}
