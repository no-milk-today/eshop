package ru.yandex.example.spring.data.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.yandex.example.spring.data.jdbc.config.MySqlTestcontainer;

@SpringBootTest
@Testcontainers
@ImportTestcontainers(MySqlTestcontainer.class)
@ActiveProfiles("test")
public class SpringDataJdbcApplicationTest {

    @Test
    void contextLoads() {
    }
}
