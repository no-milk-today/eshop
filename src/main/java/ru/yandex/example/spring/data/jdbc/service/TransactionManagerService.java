package ru.yandex.example.spring.data.jdbc.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import ru.yandex.example.spring.data.jdbc.entity.Account;
import ru.yandex.example.spring.data.jdbc.repository.AccountRepository;

import java.math.BigDecimal;

@Service
public class TransactionManagerService {
    private final PlatformTransactionManager transactionManager;
    private final AccountRepository accountRepository;

    public TransactionManagerService(PlatformTransactionManager transactionManager,
                                     AccountRepository accountRepository) {
        this.transactionManager = transactionManager;
        this.accountRepository = accountRepository;
    }

    public void transfer(Account source, Account target, BigDecimal amount) {
        // Открываем транзакцию с конфигурацией по умолчанию
        var transaction = transactionManager.getTransaction(TransactionDefinition.withDefaults());
        try {
            // Увеличиваем баланс получателя и сохраняем
            target.setBalance(target.getBalance().add(amount));
            accountRepository.save(target);

            // Уменьшаем баланс отправителя и сохраняем
            source.setBalance(source.getBalance().subtract(amount));
            accountRepository.save(source);

            // Подтверждаем коммит транзакции в случае отсутствия ошибок
            transactionManager.commit(transaction);
        } catch (Exception e) {
            e.printStackTrace();
            // Делаем откат в случае любой ошибки (например, отрицательный баланс отправителя)
            transactionManager.rollback(transaction);
        }
    }
}

