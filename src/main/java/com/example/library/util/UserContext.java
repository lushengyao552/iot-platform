package com.example.library.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文
 *
 * <p>使用 ThreadLocal 存储当前请求的用户信息，确保线程安全。
 * 在 JWT 拦截器中设置，在 Controller/Service 中通过 getCurrentUser() 获取。
 *
 * <p>注意：必须在请求结束时调用 clear() 清除 ThreadLocal，防止内存泄漏。
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> CURRENT_USER = new ThreadLocal<>();

    /**
     * 设置当前登录用户
     */
    public static void setCurrentUser(LoginUser user) {
        CURRENT_USER.set(user);
    }

    /**
     * 获取当前登录用户
     */
    public static LoginUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        LoginUser user = CURRENT_USER.get();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        LoginUser user = CURRENT_USER.get();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 获取当前用户角色
     */
    public static String getCurrentUserRole() {
        LoginUser user = CURRENT_USER.get();
        return user != null ? user.getRole() : null;
    }

    /**
     * 判断当前用户是否为管理员
     */
    public static boolean isAdmin() {
        LoginUser user = CURRENT_USER.get();
        return user != null && "ADMIN".equals(user.getRole());
    }

    /**
     * 清除当前用户信息（必须在请求结束时调用）
     */
    public static void clear() {
        CURRENT_USER.remove();
    }

    /**
     * 登录用户信息封装
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginUser {
        /** 用户ID */
        private Long userId;
        /** 用户名 */
        private String username;
        /** 昵称 */
        private String nickname;
        /** 角色：ADMIN/USER */
        private String role;
    }
}
