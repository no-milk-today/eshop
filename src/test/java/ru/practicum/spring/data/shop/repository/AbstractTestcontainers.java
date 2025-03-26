package ru.practicum.spring.data.shop.repository;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.command.CommandScope;
import liquibase.command.core.helpers.DatabaseChangelogCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
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

    @ServiceConnection //  configures the connection automatically
    @Container
    protected static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:16.0");

    @BeforeAll
    static void beforeAll() {
        // Create a JDBC connection to the PostgreSQL container.
        try (Connection connection = postgreSQLContainer.createConnection("")) {
            // Convert the connection for Liquibase compatibility.
            var jdbcConnection = new JdbcConnection(connection);
            // Detect the correct database impl for Liquibase.
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(jdbcConnection);

            // Execute the Liquibase "update" command to apply the database changelog.
            new CommandScope("update")
                    .addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
                    .addArgumentValue(DatabaseChangelogCommandStep.CHANGELOG_FILE_ARG, "db/changelog/liquibase/db.changelog-master.xml")
                    .addArgumentValue(DatabaseChangelogCommandStep.CONTEXTS_ARG, new Contexts().toString())
                    .addArgumentValue(DatabaseChangelogCommandStep.LABEL_FILTER_ARG, new LabelExpression().getOriginalString())
                    .execute();
        } catch (Exception e) {
            // Convert any exception to a RuntimeException, indicating a failure in the migration process.
            throw new RuntimeException("Liquibase migration failed", e);
        }
    }
}
