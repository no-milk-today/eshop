package ru.yandex.example.spring.data.jdbc.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.example.spring.data.jdbc.entity.Account;
import ru.yandex.example.spring.data.jdbc.repository.AccountRepository;

import java.math.BigDecimal;

@Service
public class TransactionTemplateService {
    private final AccountRepository accountRepository;
    private final TransactionTemplate transactionTemplate; // для выполнения операций внутри сервиса в транзакции

    public TransactionTemplateService(PlatformTransactionManager transactionManager,
                                      AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED); // optional
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); // optional
    }

    public void transfer(Account source, Account target, BigDecimal amount) {
        transactionTemplate.executeWithoutResult(status -> {
            // Увеличиваем баланс получателя и сохраняем
            target.setBalance(target.getBalance().add(amount));
            accountRepository.save(target);

            // Уменьшаем баланс отправителя и сохраняем
            source.setBalance(source.getBalance().subtract(amount));
            accountRepository.save(source);
        });
    }
}

