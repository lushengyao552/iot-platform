package com.example.library;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot 应用启动测试
 *
 * <p>@SpringBootTest 会加载完整的 Spring 应用上下文，
 * 验证所有 Bean 是否能正确注入和初始化。
 *
 * <p>这是最基础的集成测试，确保应用配置没有问题。
 */
@SpringBootTest
class LibraryApplicationTests {

    @Test
    void contextLoads() {
        // 如果应用上下文加载失败，此测试会抛出异常
        // 空方法体即可验证上下文能否正常启动
    }
}
