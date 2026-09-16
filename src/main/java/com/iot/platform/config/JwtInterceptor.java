package com.iot.platform.config;

import com.iot.platform.common.exception.BusinessException;
import com.iot.platform.common.result.ResultCode;
import com.iot.platform.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String auth = request.getHeader("Authorization");
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Claims claims = jwtUtil.parseToken(auth.substring(7));
        if (claims == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        UserContext.set(UserContext.LoginUser.builder()
                .userId(claims.get("userId", Long.class))
                .username(claims.getSubject())
                .role(claims.get("role", String.class))
                .build());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
