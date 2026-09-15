package com.example.library.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserContext {

    private static final ThreadLocal<LoginUser> CURRENT_USER = new ThreadLocal<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginUser {
        private Long userId;
        private String username;
        private String nickname;
        private String role;
    }

    public static void setCurrentUser(LoginUser user) {
        CURRENT_USER.set(user);
    }

    public static LoginUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static Long getCurrentUserId() {
        LoginUser user = CURRENT_USER.get();
        return user == null ? null : user.getUserId();
    }

    public static String getCurrentUsername() {
        LoginUser user = CURRENT_USER.get();
        return user == null ? null : user.getUsername();
    }

    public static String getCurrentUserRole() {
        LoginUser user = CURRENT_USER.get();
        return user == null ? null : user.getRole();
    }

    public static boolean isAdmin() {
        LoginUser user = CURRENT_USER.get();
        return user != null && "ADMIN".equals(user.getRole());
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
