package com.aiforaso.platform.security;

import com.aiforaso.platform.domain.PlatformUser;

public final class AuthContext {
    private static final ThreadLocal<PlatformUser> currentUser = new ThreadLocal<>();

    private AuthContext() {}

    public static void setCurrentUser(PlatformUser user) {
        currentUser.set(user);
    }

    public static PlatformUser getCurrentUser() {
        return currentUser.get();
    }

    public static Long getCurrentUserId() {
        PlatformUser user = currentUser.get();
        return user != null ? user.getId() : null;
    }

    public static String getCurrentUserRole() {
        PlatformUser user = currentUser.get();
        return user != null ? user.getRole() : null;
    }

    public static boolean isAdmin() {
        String role = getCurrentUserRole();
        return "ADMIN".equals(role);
    }

    public static void clear() {
        currentUser.remove();
    }
}
