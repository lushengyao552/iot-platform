package com.example.library.interceptor;

import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.ResultCode;
import com.example.library.util.JwtUtil;
import com.example.library.util.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 *
 * <p>拦截需要登录的请求，从请求头中获取 Token 并验证：
 * <ol>
 *   <li>从 Authorization 请求头获取 Token</li>
 *   <li>验证 Token 格式和有效性</li>
 *   <li>解析用户信息并存入 UserContext</li>
 *   <li>请求结束后清除 UserContext</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Value("${library.jwt.header}")
    private String header;

    @Value("${library.jwt.prefix}")
    private String prefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从请求头获取 Token
        String authHeader = request.getHeader(header);
        if (authHeader == null || authHeader.isEmpty()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 验证 Token 前缀
        if (!authHeader.startsWith(prefix)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 提取 Token（去掉 "Bearer " 前缀）
        String token = authHeader.substring(prefix.length());

        // 验证 Token 有效性
        if (!jwtUtil.validateToken(token)) {
            if (jwtUtil.isTokenExpired(token)) {
                throw new BusinessException(ResultCode.TOKEN_EXPIRED);
            }
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 解析用户信息
        Claims claims = jwtUtil.getClaimsFromToken(token);
        if (claims == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        String nickname = claims.get("nickname", String.class);

        // 存入用户上下文
        UserContext.LoginUser loginUser = UserContext.LoginUser.builder()
                .userId(userId)
                .username(username)
                .nickname(nickname)
                .role(role)
                .build();
        UserContext.setCurrentUser(loginUser);

        log.debug("用户认证通过: userId={}, username={}, role={}", userId, username, role);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束后清除 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}
