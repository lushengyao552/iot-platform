package com.example.library.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis 服务工具类
 *
 * <p>封装常用的 Redis 操作：
 * <ul>
 *   <li>缓存操作：set/get/delete/expire</li>
 *   <li>分布式锁：tryLock/unlock（基于 SETNX + Lua 脚本保证原子性）</li>
 *   <li>计数器：increment/decrement</li>
 * </ul>
 *
 * <p>分布式锁原理：
 * <ol>
 *   <li>加锁：SET key value NX EX timeout（原子操作，key不存在才设置成功）</li>
 *   <li>value 使用唯一标识（如 UUID），防止误删别人的锁</li>
 *   <li>解锁：使用 Lua 脚本判断 value 是否匹配，匹配才删除（保证原子性）</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 解锁 Lua 脚本：先判断 value 是否匹配，匹配才删除（保证原子性）
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end";

    // ============================================================
    // 通用缓存操作
    // ============================================================

    /**
     * 设置缓存（永不过期）
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取缓存并转换为指定类型
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 判断 key 是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    // ============================================================
    // 分布式锁
    // ============================================================

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey 锁的 key
     * @param value   锁的 value（唯一标识，用于解锁时校验）
     * @param timeout 锁的过期时间（防止死锁）
     * @param unit    时间单位
     * @return true-获取锁成功，false-获取锁失败
     */
    public Boolean tryLock(String lockKey, String value, long timeout, TimeUnit unit) {
        try {
            // setIfAbsent = SET NX（key 不存在才设置成功），同时设置过期时间
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, value, timeout, unit);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("获取分布式锁失败, lockKey={}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放分布式锁
     *
     * <p>使用 Lua 脚本保证原子性：先判断 value 是否匹配，匹配才删除。
     * 防止误删其他线程/进程的锁。
     *
     * @param lockKey 锁的 key
     * @param value   锁的 value（必须与加锁时一致）
     * @return true-释放成功，false-释放失败（value不匹配或key不存在）
     */
    public Boolean unlock(String lockKey, String value) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(UNLOCK_SCRIPT);
            script.setResultType(Long.class);

            Long result = redisTemplate.execute(
                    script,
                    Collections.singletonList(lockKey),
                    value
            );
            return result != null && result == 1;
        } catch (Exception e) {
            log.error("释放分布式锁失败, lockKey={}", lockKey, e);
            return false;
        }
    }

    // ============================================================
    // 计数器
    // ============================================================

    /**
     * 原子递增
     *
     * @param key   计数器 key
     * @param delta 增量
     * @return 递增后的值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 原子递减
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }
}
