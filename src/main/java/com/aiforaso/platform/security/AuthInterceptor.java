package com.aiforaso.platform.security;

import java.util.Base64;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.aiforaso.platform.domain.PlatformUser;
import com.aiforaso.platform.repository.PlatformUserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final PlatformUserRepository platformUserRepository;

    public AuthInterceptor(PlatformUserRepository platformUserRepository) {
        this.platformUserRepository = platformUserRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String decoded = new String(Base64.getDecoder().decode(token));
                String email = decoded.split(":")[0];
                platformUserRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                    if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
                        AuthContext.setCurrentUser(user);
                    }
                });
            } catch (Exception e) {
                // Invalid token format, ignore
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        AuthContext.clear();
    }
}
