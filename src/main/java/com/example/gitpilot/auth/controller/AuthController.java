package com.example.gitpilot.auth.controller;

import com.example.gitpilot.user.dto.UserResponse;
import com.example.gitpilot.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Endpoints for user authentication and context")
@RestController
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get current authenticated user", description = "Returns details of the currently logged in GitHub user")
    @GetMapping("/me")
    public UserResponse currentUser(@AuthenticationPrincipal OAuth2User user){
        return userService.synchronizeUser(user);
    }
}
