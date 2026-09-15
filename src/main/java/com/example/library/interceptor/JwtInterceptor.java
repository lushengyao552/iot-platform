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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestURI = request.getRequestURI();
        log.debug("请求: {} {}", request.getMethod(), requestURI);

        String authHeader = request.getHeader(HEADER);
        if (!StringUtils.hasText(authHeader)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        if (!authHeader.startsWith(PREFIX)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        String token = authHeader.substring(PREFIX.length());

        if (!jwtUtil.validateToken(token)) {
            if (jwtUtil.isTokenExpired(token)) {
                throw new BusinessException(ResultCode.TOKEN_EXPIRED);
            }
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        Claims claims = jwtUtil.getClaimsFromToken(token);
        if (claims != null) {
            UserContext.LoginUser user = UserContext.LoginUser.builder()
                    .userId(claims.get("userId", Long.class))
                    .username(claims.getSubject())
                    .role(claims.get("role", String.class))
                    .build();
            UserContext.setCurrentUser(user);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
