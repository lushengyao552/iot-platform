package com.example.library.common.config;

import com.example.library.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类
 *
 * <p>配置内容：
 * <ul>
 *   <li>跨域（CORS）配置</li>
 *   <li>JWT 拦截器注册与白名单</li>
 *   <li>静态资源映射</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /**
     * 配置跨域
     *
     * <p>允许前端开发服务器（如 Vue/React 项目）跨域访问本接口。
     * 生产环境应限制为具体域名，不要使用 "*"。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 注册拦截器
     *
     * <p>JWT 拦截器拦截所有请求，但以下路径放行（白名单）：
     * <ul>
     *   <li>/auth/**：登录、注册等认证接口</li>
     *   <li>/doc.html、/v3/api-docs/**、/swagger-ui/**、/webjars/**：接口文档</li>
     *   <li>/error：错误页面</li>
     * </ul>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",
                        "/doc.html",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/error"
                );
    }
}
