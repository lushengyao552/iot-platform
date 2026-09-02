package com.example.library;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 图书管理系统启动类
 *
 * <p>@SpringBootApplication 是 Spring Boot 的核心注解，组合了：
 * <ul>
 *   <li>@Configuration：标记为配置类</li>
 *   <li>@EnableAutoConfiguration：开启自动配置</li>
 *   <li>@ComponentScan：开启组件扫描</li>
 * </ul>
 *
 * <p>@MapperScan 扫描 MyBatis Mapper 接口，自动生成代理实现
 */
@SpringBootApplication
@MapperScan("com.example.library.mapper")
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
        System.out.println("\n" +
                "  _     _ _                            \n" +
                " | |   (_) |__  _ __ __ _ _ __ _   _  \n" +
                " | |   | | '_ \\| '__/ _` | '__| | | | \n" +
                " | |___| | |_) | | | (_| | |  | |_| | \n" +
                " |_____|_|_.__/|_|  \\__,_|_|   \\__, | \n" +
                "                                 |___/  \n" +
                "  图书管理系统启动成功！\n" +
                "  接口文档地址: http://localhost:8080/api/doc.html\n");
    }
}
