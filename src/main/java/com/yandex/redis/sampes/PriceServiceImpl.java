package com.yandex.redis.sampes;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.function.Supplier;

@Service
public class PriceServiceImpl implements IPriceService {

    @Cacheable(
            value = "car-prices",               // Имя кеша и первая часть ключа
            key = "#name"   // Вторая часть ключа (берётся по имени из аргумента)
    )
    @Override
    public BigDecimal computePrice(String name, Supplier<BigDecimal> priceSupplier) {
        // Прокси перед вызовом метода проверяет, есть ли значение в кеше
        // Если значения нет, то вызывается код ниже, иначе — значение берётся из кеша
        System.out.println("Значение в кеше отсутствует. Вычисляю...");
        return priceSupplier.get();
    }

    @CacheEvict(
            value = "car-prices", // Имя кеша
            allEntries = true  // Удаление всех записей
    )
    @Override
    public void clearPricesCache() {
        System.out.println("Кеш очищен");
    }

    @CacheEvict(
            value = "car-prices",               // Имя кеша и первая часть ключа
            key = "#name"   // Вторая часть ключа (берётся по имени из аргумента)
    )
    @Override
    public void evictPriceFromCache(String name) {
        System.out.println("Запись очищена по ключу");
    }

    @CachePut(value = "car-prices", key = "#carPrice")
    @Override
    public BigDecimal upsertPriceInCache(String name, BigDecimal carPrice) {
        // Обновляем данные о цене
        return carPrice;
    }
}
