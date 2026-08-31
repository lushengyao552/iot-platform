package com.example.library.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * <p>核心配置：
 * <ul>
 *   <li>Key 序列化：StringRedisSerializer（字符串）</li>
 *   <li>Value 序列化：Jackson2JsonRedisSerializer（JSON，可读性好、跨语言）</li>
 *   <li>Hash Key/Value：同样使用 JSON 序列化</li>
 * </ul>
 *
 * <p>默认的 JDK 序列化存在问题：
 * <ul>
 *   <li>序列化后是二进制，无法在 Redis 客户端直接查看</li>
 *   <li>需要实体类实现 Serializable 接口</li>
 *   <li>序列化体积大，性能差</li>
 * </ul>
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 创建 JSON 序列化器
        Jackson2JsonRedisSerializer<Object> jsonSerializer = createJsonSerializer();

        // Key 使用字符串序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 设置序列化方式
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 初始化序列化配置
        template.afterPropertiesSet();

        return template;
    }

    /**
     * 创建 Jackson JSON 序列化器
     */
    private Jackson2JsonRedisSerializer<Object> createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 支持 Java 8 时间类型（LocalDateTime、LocalDate 等）
        objectMapper.registerModule(new JavaTimeModule());

        // 设置可见性：允许序列化所有字段
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 记录类型信息：反序列化时能还原具体类型（非默认 Object）
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }
}
