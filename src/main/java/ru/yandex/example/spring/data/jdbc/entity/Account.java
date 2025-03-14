package ru.yandex.example.spring.data.jdbc.entity;

import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

public class Account{
    @Id
    private Integer id;
    private String name;
    private BigDecimal balance = BigDecimal.valueOf(10_000L);

    public Account() {
    }

    public Account(String name) {
        this.name = name;
    }

    public Account(Integer id, String name, BigDecimal balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "Account{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", balance=" + balance +
            '}';
    }
}
