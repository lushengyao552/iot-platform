package com.example.library.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 *
 * <p>配置内容：
 * <ul>
 *   <li>分页插件：自动拦截分页查询，生成 count SQL 和分页 SQL</li>
 *   <li>乐观锁插件：通过 @Version 注解实现乐观锁</li>
 * </ul>
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件（指定数据库类型为 MySQL）
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大条数限制（防止恶意查询）
        paginationInterceptor.setMaxLimit(500L);
        // 溢出总页数后是否进行处理（默认 false，true 表示溢出后返回空）
        paginationInterceptor.setOverflow(false);
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }
}
