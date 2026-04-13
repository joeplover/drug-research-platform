package com.aiforaso.platform.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiforaso.platform.dto.UserCreateRequest;
import com.aiforaso.platform.dto.UserLoginRequest;
import com.aiforaso.platform.dto.UserRegisterRequest;
import com.aiforaso.platform.dto.UserRoleUpdateRequest;
import com.aiforaso.platform.dto.UserSessionView;
import com.aiforaso.platform.dto.UserView;
import com.aiforaso.platform.security.AuthContext;
import com.aiforaso.platform.service.OperationLogService;
import com.aiforaso.platform.service.UserService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final OperationLogService operationLogService;

    public UserController(UserService userService, OperationLogService operationLogService) {
        this.userService = userService;
        this.operationLogService = operationLogService;
    }

    @PostMapping
    public UserView create(@Valid @RequestBody UserCreateRequest request) {
        requireAdmin();
        UserView user = userService.create(request);
        operationLogService.record(user.email(), "CREATE", "USER", String.valueOf(user.id()), "Created platform user");
        return user;
    }

    @GetMapping
    public List<UserView> list() {
        requireAdmin();
        return userService.list();
    }

    @PostMapping("/login")
    public UserSessionView login(@Valid @RequestBody UserLoginRequest request) {
        UserSessionView session = userService.login(request);
        operationLogService.record(session.email(), "LOGIN", "USER", String.valueOf(session.userId()), "User login");
        return session;
    }

    @PostMapping("/register")
    public UserSessionView register(@Valid @RequestBody UserRegisterRequest request) {
        UserSessionView session = userService.register(request);
        operationLogService.record(session.email(), "REGISTER", "USER", String.valueOf(session.userId()), "User registered");
        return session;
    }

    @PatchMapping("/{id}/role")
    public UserView updateRole(@PathVariable Long id, @Valid @RequestBody UserRoleUpdateRequest request) {
        requireAdmin();
        UserView user = userService.updateRole(id, request);
        operationLogService.record(user.email(), "UPDATE_ROLE", "USER", String.valueOf(user.id()), "Updated role/status");
        return user;
    }

    private void requireAdmin() {
        if (!AuthContext.isAdmin()) {
            throw new IllegalArgumentException("需要管理员权限");
        }
    }
}
