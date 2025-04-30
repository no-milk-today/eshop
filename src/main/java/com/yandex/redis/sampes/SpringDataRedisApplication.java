package com.yandex.redis.sampes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableCaching
public class SpringDataRedisApplication implements ApplicationRunner {

    @Autowired
    private PopularArticlesCacheService popularArticlesCacheService;
    @Autowired
    private CarPriceRepository carPriceRepository;
    @Autowired
    private IPriceService priceService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Кешируем информацию о машине Василия...");
        carPriceRepository.save(new CarPrice("МашинаВасилия", BigDecimal.valueOf(1_000_00)));

        System.out.println("Проверяем записанные в Redis машины — " + carPriceRepository.findAll());

        System.out.println("Ждём, пока пройдёт TTL (12 секунд)");
        TimeUnit.SECONDS.sleep(12);

        System.out.println("По прошествии TTL в Redis остались машины — " + carPriceRepository.findAll());

        // task 2
        String isbn = "123-456-789";
        String articleContent = "This is a popular article.";

        System.out.println("Caching the article...");
        popularArticlesCacheService.cache(isbn, articleContent);

        System.out.println("Retrieving the article from cache...");
        String cachedArticle = popularArticlesCacheService.getArticle(isbn);
        System.out.println("Cached article content: " + cachedArticle);

        System.out.println("Waiting for TTL to expire (1 day simulated)...");
        Thread.sleep(2000); // Simulate a short wait for demonstration purposes

        System.out.println("Retrieving the article again to extend TTL...");
        cachedArticle = popularArticlesCacheService.getArticle(isbn);
        System.out.println("Cached article content after extending TTL: " + cachedArticle);

        // Price service caching usage.
        String carName = "BMW";
        BigDecimal computedPrice = priceService.computePrice(carName, () -> {
            System.out.println("Value missing in cache. Computing price for " + carName + "...");
            return BigDecimal.valueOf(20000);
        });
        System.out.println("Computed price for " + carName + ": " + computedPrice);

        // Update cached price.
        BigDecimal updatedPrice = BigDecimal.valueOf(21000);
        priceService.upsertPriceInCache(carName, updatedPrice);
        System.out.println("Updated cached price for " + carName + ": " + priceService.computePrice(carName, () -> updatedPrice));

        // Evict price from cache and compute price again.
        priceService.evictPriceFromCache(carName);
        BigDecimal newComputedPrice = priceService.computePrice(carName, () -> {
            System.out.println("Cache cleared. Recomputing price for " + carName + "...");
            return BigDecimal.valueOf(20000);
        });
        System.out.println("Recomputed price for " + carName + " after cache eviction: " + newComputedPrice);

    }

    public static void main(String[] args) {
        SpringApplication.run(SpringDataRedisApplication.class, args);
    }
}
