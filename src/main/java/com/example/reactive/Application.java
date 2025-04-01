package com.example.reactive;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;

public class Application {

    static class YandexPracticum {
        @Column("v")
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }


    public static void main(String[] args) {
        ConnectionFactory connectionFactory =
                ConnectionFactories.get("r2dbc:h2:mem:///practicum?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        var template = new R2dbcEntityTemplate(connectionFactory);

        // Вызов метода block противоречит реактивному программированию.
        // В данном примере он нужен, чтобы гарантировать последовательность действий.
        template.getDatabaseClient()
                .sql("create table if not exists practicum(v varchar(255))")
                .then()
                .block();

        var entity = new YandexPracticum();
        entity.setValue("Yandex.Practicum");
        template.insert(YandexPracticum.class)
                .into("practicum")
                .using(entity)
                .block();

        template.select(YandexPracticum.class)
                .from("practicum")
                .matching(Query.query(Criteria.where("value").isNotNull()))
                .one()
                .map(YandexPracticum::getValue)
                .map(String::toLowerCase)
                .map(it -> it.replace(".", " "))
                .subscribe(System.out::println);
    }

}

