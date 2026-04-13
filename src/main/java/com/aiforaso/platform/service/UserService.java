package com.aiforaso.platform.service;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiforaso.platform.domain.PlatformUser;
import com.aiforaso.platform.dto.UserCreateRequest;
import com.aiforaso.platform.dto.UserLoginRequest;
import com.aiforaso.platform.dto.UserRegisterRequest;
import com.aiforaso.platform.dto.UserRoleUpdateRequest;
import com.aiforaso.platform.dto.UserSessionView;
import com.aiforaso.platform.dto.UserView;
import com.aiforaso.platform.repository.PlatformUserRepository;

@Service
public class UserService {

    private final PlatformUserRepository platformUserRepository;
    private final PasswordHashService passwordHashService;

    public UserService(PlatformUserRepository platformUserRepository, PasswordHashService passwordHashService) {
        this.platformUserRepository = platformUserRepository;
        this.passwordHashService = passwordHashService;
    }

    @Transactional
    public UserView create(UserCreateRequest request) {
        platformUserRepository.findByEmailIgnoreCase(request.email()).ifPresent(existing -> {
            throw new IllegalArgumentException("User email already exists: " + existing.getEmail());
        });

        PlatformUser user = new PlatformUser();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordHashService.hash(request.password()));
        user.setRole(request.role());
        user.setStatus("ACTIVE");
        return toView(platformUserRepository.save(user));
    }

    @Transactional
    public UserSessionView register(UserRegisterRequest request) {
        UserView user = create(new UserCreateRequest(
                request.username(),
                request.email(),
                request.password(),
                "RESEARCHER"));
        String token = Base64.getEncoder().encodeToString((user.email() + ":" + UUID.randomUUID()).getBytes());
        return new UserSessionView(user.id(), user.username(), user.email(), user.role(), token);
    }

    @Transactional(readOnly = true)
    public List<UserView> list() {
        return platformUserRepository.findAll().stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public UserSessionView login(UserLoginRequest request) {
        PlatformUser user = platformUserRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.email()));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("User is not active: " + request.email());
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException("Password is not initialized for this account");
        }
        if (!passwordHashService.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect password");
        }
        String token = Base64.getEncoder().encodeToString((user.getEmail() + ":" + UUID.randomUUID()).getBytes());
        return new UserSessionView(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), token);
    }

    @Transactional
    public void ensureSeedUser(String username, String email, String rawPassword, String role) {
        PlatformUser user = platformUserRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            PlatformUser created = new PlatformUser();
            created.setUsername(username);
            created.setEmail(email);
            created.setRole(role);
            created.setStatus("ACTIVE");
            return created;
        });
        user.setUsername(username);
        user.setRole(role);
        user.setStatus(user.getStatus() == null || user.getStatus().isBlank() ? "ACTIVE" : user.getStatus());
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            user.setPasswordHash(passwordHashService.hash(rawPassword));
        }
        platformUserRepository.save(user);
    }

    @Transactional
    public UserView updateRole(Long id, UserRoleUpdateRequest request) {
        PlatformUser user = platformUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setRole(request.role());
        user.setStatus(request.status());
        return toView(platformUserRepository.save(user));
    }

    private UserView toView(PlatformUser user) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
