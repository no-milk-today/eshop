package com.yandex.reactive.testcontainers.reshop;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test") // чтобы читать данные с тестового application.yml
public abstract class AbstractTestContainerTest {

    @Container // декларируем объект учитываемым тест-контейнером
    @ServiceConnection // автоматически назначаем параметры соединения с контейнером
    static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>("mysql:8.0.28");

}