package com.iot.platform.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserContext {
    private static final ThreadLocal<LoginUser> CURRENT = new ThreadLocal<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginUser {
        private Long userId;
        private String username;
        private String role;
    }

    public static void set(LoginUser user) { CURRENT.set(user); }
    public static LoginUser get() { return CURRENT.get(); }
    public static Long getUserId() { return CURRENT.get() == null ? null : CURRENT.get().getUserId(); }
    public static boolean isAdmin() {
        LoginUser u = CURRENT.get();
        return u != null && "ADMIN".equals(u.getRole());
    }
    public static void clear() { CURRENT.remove(); }
}
