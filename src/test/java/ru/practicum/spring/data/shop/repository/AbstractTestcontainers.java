package ru.practicum.spring.data.shop.repository;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.command.CommandScope;
import liquibase.command.core.helpers.DatabaseChangelogCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;


@Testcontainers
public abstract class AbstractTestcontainers {

    @ServiceConnection
    @Container
    protected static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:16.0");

    @BeforeAll
    static void beforeAll() {
        try (Connection connection = postgreSQLContainer.createConnection("")) {
            // Оборачиваем стандартное соединение в объект JdbcConnection,
            // который требуется Liquibase для работы с базой данных.
            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            // Получаем объект Database, соответствующий типу подключенной базы данных.
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(jdbcConnection);

            // CommandScope позволяет программно собрать и выполнить команду Liquibase (аналог вызова liquibase update).
            new CommandScope("update")
                    // Передаём объект Database, полученный из контейнера.
                    .addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
                    // Указываем путь к master-описанию миграций.
                    .addArgumentValue(DatabaseChangelogCommandStep.CHANGELOG_FILE_ARG, "db/changelog/liquibase/db.changelog-master.xml")
                    // Передаём контексты (в данном случае – стандартное представление пустых контекстов).
                    .addArgumentValue(DatabaseChangelogCommandStep.CONTEXTS_ARG, new Contexts().toString())
                    // Передаём фильтр меток (Label Expression), который по умолчанию пуст.
                    .addArgumentValue(DatabaseChangelogCommandStep.LABEL_FILTER_ARG, new LabelExpression().getOriginalString())
                    // Выполняем команду, которая применяет миграции к базе данных.
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException("Liquibase migration failed", e);
        }
    }
}
