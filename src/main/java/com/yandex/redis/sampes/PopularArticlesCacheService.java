package com.yandex.redis.sampes;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class PopularArticlesCacheService implements PopularArticleAware {

    private static final String keyspaceKey = "articles";
    private static final Duration ttl = Duration.ofDays(1);

    private final StringRedisTemplate stringRedisTemplate;

    public PopularArticlesCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void cache(String isbnNumber, String articleContent) {
        stringRedisTemplate.opsForValue().set(
                buildKey(isbnNumber),
                articleContent,
                ttl.toDays(),
                TimeUnit.DAYS
        );
    }

    @Override
    public String getArticle(String isbnNumber) {
        var result = stringRedisTemplate.opsForValue().get(buildKey(isbnNumber));
        stringRedisTemplate.expire(
                buildKey(isbnNumber),
                ttl.toDays(),
                TimeUnit.DAYS
        );
        return result;
    }

    private static String buildKey(String isbnNumber) {
        return String.join(":", keyspaceKey, isbnNumber);
    }
}
