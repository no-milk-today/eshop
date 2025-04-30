package com.yandex.redis.sampes;

import java.math.BigDecimal;
import java.util.function.Supplier;

public interface IPriceService {

    /**
     * Получить значение цены товара:
     * * из кеша prices, если оно имеется,
     * * вычислить из Supplier и положить в кеш
     */
    BigDecimal computePrice(String name, Supplier<BigDecimal> price);

    /**
     * Очистить весь кеш prices
     */
    void clearPricesCache();

    /**
     * Удалить значение из кеша prices по ключу
     */
    void evictPriceFromCache(String name);

    /**
     * Выполнить апсерт в кеш car-prices
     */
    BigDecimal upsertPriceInCache(String name, BigDecimal carPrice);

}
