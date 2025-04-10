package com.yandex.reactive.testcontainers.testconainers_demo_raactive;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.Arrays;

@Service
public class AccountService {

    private final AccountR2dbcRepository accountRepository;

    public AccountService(AccountR2dbcRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Flux<Account> saveAll(Account... accounts) {
        return accountRepository.saveAll(Arrays.stream(accounts).toList());
    }

    public Flux<Account> findRichAccounts(BigDecimal min) {
        return accountRepository.findAccountsByBalanceGreaterThan(min);
    }
}
