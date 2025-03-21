package ru.practicum.spring.data.shop.repository;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractDaoTest extends AbstractTestcontainers {

    @BeforeAll
    static void setUp() {
        assertNotNull(postgreSQLContainer.getJdbcUrl());
        assertNotNull(postgreSQLContainer.getUsername());
        assertNotNull(postgreSQLContainer.getPassword());
    }
}