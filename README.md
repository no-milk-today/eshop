### initial structure

Пока описаны entities в БД (директория domain/entity)
Подключен и настроен Liquibase

**На что обратить внимание:**
1. Все файлы миграций находятся в директории classpath:/db/changelog/liquibase и эта директория установлена в настройках
2. Все файлы миграций имеют preconditions и rollbacks.
4. Создавется структура БД средствами Liquibase, без различные ORM (в том числе Hibernate). postgres db
5. Используется testcontainers (postgres)

