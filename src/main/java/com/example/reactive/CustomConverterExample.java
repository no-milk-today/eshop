package com.example.reactive;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.r2dbc.core.Parameter;

import java.util.List;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

@Slf4j
@SpringBootApplication
public class CustomConverterExample {

    @Configuration
    static class R2dbcConfiguration extends AbstractR2dbcConfiguration {

        @Override
        @Bean
        public ConnectionFactory connectionFactory() {
            return ConnectionFactories.get("r2dbc:h2:mem:///practicum?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        }

        @Override
        protected List<Object> getCustomConverters() {
            // register converters
            return List.of(
                    new PersonReadConverter(),
                    new PersonWriteConverter()
            );
        }
    }

    static class PersonReadConverter implements Converter<Row, Person> {
        @Override
        public Person convert(Row source) {
            Person person = new Person();
            person.setId(source.get("id", Long.class));
            person.setFirstName(source.get("first_name", String.class));
            person.setSecondName(source.get("second_name", String.class));
            return person;
        }
    }

    static class PersonWriteConverter implements Converter<Person, OutboundRow> {
        @Override
        public OutboundRow convert(Person source) {
            OutboundRow row = new OutboundRow()
                    .append("first_name", Parameter.from(source.getFirstName()))
                    .append("second_name", Parameter.from(source.getSecondName()));

            if (source.getId() != null) {
                row = row.append("id", Parameter.from(source.getId()));
            }

            return row;
        }
    }

    @Table("persons")
    static class Person {
        @Id        private Long id;
        private String firstName;
        private String secondName;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getSecondName() {
            return secondName;
        }

        public void setSecondName(String secondName) {
            this.secondName = secondName;
        }
    }

    public static void main(String[] args) {
        var appContext = SpringApplication.run(Application.class);
        var template = appContext.getBean(R2dbcEntityTemplate.class);

        template.getDatabaseClient()
                .sql("create table if not exists persons(id bigint primary key, first_name text not null, second_name text not null)")
                .then()
                .block();

        Person person = new Person();
        person.setId(1L);
        person.setFirstName("First Name");
        person.setSecondName("Second Name");

        template.insert(person).block();

        template.select(Person.class)
                .matching(query(where("id").is(1L)))
                .one()
                .subscribe(it -> {
                    System.out.println(it.getId()); // 1
                    System.out.println(it.getFirstName()); // First Name
                    System.out.println(it.getSecondName()); // Second Name
                });
    }

}