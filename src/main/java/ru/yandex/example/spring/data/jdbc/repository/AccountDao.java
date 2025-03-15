package ru.yandex.example.spring.data.jdbc.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.example.spring.data.jdbc.entity.Account;

import java.sql.ResultSet;
import java.util.List;

@Repository
public class AccountDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Account> accountRowMapper = (ResultSet rs, int rowNum) -> new Account(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getBigDecimal("balance")
    );

    public AccountDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Account> findAll() {
        String sql = "select * from account";
        return jdbcTemplate.query(sql, accountRowMapper);
    }

    public Account findFirstByName(String name) {
        String sql = "select * from account where name = ? limit 1";
        return jdbcTemplate.queryForObject(sql, accountRowMapper, name);
    }

    public void create(String name) {
        jdbcTemplate.update( // PreparedStatement behind the scenes
                "INSERT INTO account (name) VALUES (?)",
                name
        );
    }

    public void update(Account account) {
        jdbcTemplate.update( // PreparedStatement behind the scenes
                "UPDATE account SET name = ?, balance = ? WHERE id = ?",
                account.getName(),
                account.getBalance(),
                account.getId()
        );
    }

}
