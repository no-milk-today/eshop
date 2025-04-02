package com.example.reactive;

import io.r2dbc.spi.ConnectionFactories;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

public class MappingApplication {

    @Table("persons")
    static class Person {

        @Column("person_id")
        private Long id;

        // совпадает и так
        private String firstName;

        @Column("second_name")
        private String lastName;

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

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    public static void main(String[] args) {
        var connectionFactory =
                ConnectionFactories.get("r2dbc:h2:mem:///practicum?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        var template =
                new R2dbcEntityTemplate(connectionFactory);

        template.getDatabaseClient()
                .sql("create table if not exists persons(person_id bigint primary key, first_name text, second_name text)")
                .then()
                .block();

        var person = new Person();
        person.setId(1L);
        person.setFirstName("First Name");
        person.setLastName("Last Name");

        template.insert(person).block();

        template.select(Person.class)
                .matching(
                        query(where("id").is(1L))
                )
                .one()
                .subscribe(it -> {
                    System.out.println(it.getId()); // 1
                    System.out.println(it.getFirstName()); // First Name
                    System.out.println(it.getLastName()); // Last Name
                });
    }

}
