package ru.yandex.example.spring.data.jdbc.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.example.spring.data.jdbc.entity.Account;
import ru.yandex.example.spring.data.jdbc.repository.AccountRepository;

import java.math.BigDecimal;

@Service
public class TransactionalAnnotationService {

    private final AccountRepository accountRepository;

    public TransactionalAnnotationService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    void transfer(Account source, Account target, BigDecimal amount) {
        // Увеличиваем баланс получателя и сохраняем
        target.setBalance(target.getBalance().add(amount));
        accountRepository.save(target);

        // Уменьшаем баланс отправителя и сохраняем
        source.setBalance(source.getBalance().subtract(amount));
        accountRepository.save(source);
    }
}

