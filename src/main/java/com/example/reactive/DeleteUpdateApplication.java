package com.example.reactive;

import io.r2dbc.spi.ConnectionFactories;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;

import java.util.stream.LongStream;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Update.update;

@Slf4j
public class DeleteUpdateApplication {

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Person {
        private Long id;
        private String username;
        private Boolean active;
    }

    static Person testPerson(Long id) {
        return Person.builder()
                .id(id)
                .username("Test #" + id)
                .active(Boolean.TRUE)
                .build();
    }

    public static void main(String[] args) {
        var connectionFactory =
                ConnectionFactories.get("r2dbc:h2:mem:///practicum?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        var template = new R2dbcEntityTemplate(connectionFactory);

        template.getDatabaseClient()
                .sql("create table person(id bigserial not null, username varchar(128) not null, active boolean)")
                .then()
                .block();

        Flux.fromStream(LongStream.rangeClosed(1L, 10L).boxed())
                .map(DeleteUpdateApplication::testPerson)
                .flatMap(template::insert)
                .collectList()
                .block();

        // UPDATE person SET active = 'false' WHERE id >= 5
        template.update(Person.class)
                .matching(query(where("id")
                        .greaterThanOrEquals(5)))
                .apply(update("active", Boolean.FALSE))
                .subscribe(it -> log.info("Обновлено строк: {}", it)); // 6

        // SELECT всех записей перед удалением
        template.select(Person.class)
                .all()
                .doOnNext(person -> log.info("Запись: id={}, username={}, active={}",
                        person.getId(), person.getUsername(), person.getActive()))
                .blockLast(); // Ожидаем завершения выборки

        // DELETE FROM person WHERE active is true
        template.delete(Person.class)
                .matching(query(where("active").isTrue()))
                .all()
                .subscribe(it -> log.info("Удалено строк: {}", it)); // 4
    }

}