package com.yandex.reactive.testcontainers.testconainers_demo_raactive;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractDaoTest extends AbstractTestContainerTest {

    @BeforeAll
    static void setUp() {
        assertNotNull(mysqlContainer.getJdbcUrl());
        assertNotNull(mysqlContainer.getUsername());
        assertNotNull(mysqlContainer.getPassword());
    }
}
