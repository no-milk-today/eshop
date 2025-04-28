package com.yandex.redis.sampes;

public interface PopularArticleAware {

    /**
     * Положить популярную статью в кеш (ключ - идентификатор статьи)
     * Статья должна храниться одни сутки, если ни разу не была востребована
     */
    void cache(String isbnNumber, String articleContent);

    /**
     * Получить популярную статью из кеша (при вызове метода продлевать время жизни)
     */
    String getArticle(String isbnNumber);

}