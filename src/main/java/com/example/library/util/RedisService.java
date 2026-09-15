package com.example.library.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end";

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.warn("Redis set失败: {}", e.getMessage());
        }
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis set带过期失败: {}", e.getMessage());
        }
    }

    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis get失败: {}", e.getMessage());
            return null;
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        try {
            Object obj = redisTemplate.opsForValue().get(key);
            return obj == null ? null : clazz.cast(obj);
        } catch (Exception e) {
            log.warn("Redis get带类型失败: {}", e.getMessage());
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis delete失败: {}", e.getMessage());
        }
    }

    public boolean hasKey(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("Redis hasKey失败: {}", e.getMessage());
            return false;
        }
    }

    public void expire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis expire失败: {}", e.getMessage());
        }
    }

    public boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("Redis tryLock失败: {}", e.getMessage());
            return false;
        }
    }

    public boolean unlock(String key, String value) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(UNLOCK_SCRIPT);
            script.setResultType(Long.class);
            Long result = redisTemplate.execute(script, java.util.Collections.singletonList(key), value);
            return result != null && result > 0;
        } catch (Exception e) {
            log.warn("Redis unlock失败: {}", e.getMessage());
            return false;
        }
    }

    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.warn("Redis increment失败: {}", e.getMessage());
            return null;
        }
    }

    public Long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.warn("Redis decrement失败: {}", e.getMessage());
            return null;
        }
    }
}
