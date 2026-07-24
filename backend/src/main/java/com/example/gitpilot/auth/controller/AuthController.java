package com.example.gitpilot.auth.controller;

import com.example.gitpilot.user.dto.UserResponse;
import com.example.gitpilot.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Authentication", description = "Endpoints for user authentication and context")
@RestController
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get current authenticated user", description = "Returns details of the currently logged in GitHub user")
    @GetMapping("/me")
    public ResponseEntity<?> currentUser(@AuthenticationPrincipal OAuth2User user){
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", false));
        }
        UserResponse response = userService.synchronizeUser(user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Developer mock login bypass", description = "Programmatically authenticates a mock user for local evaluation/testing")
    @GetMapping("/auth/mock")
    public ResponseEntity<?> mockLogin(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        
        java.util.Map<String, Object> attributes = java.util.Map.of(
            "id", 12345678,
            "login", "gitpilot-mock-user",
            "name", "GitPilot Mock User",
            "email", "mock@gitpilot.com",
            "avatar_url", "https://avatars.githubusercontent.com/u/12345678?v=4",
            "html_url", "https://github.com/gitpilot-mock-user"
        );
        
        org.springframework.security.oauth2.core.user.OAuth2User principal = 
            new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER"),
                attributes,
                "login"
            );
            
        org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken authToken = 
            new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
                principal,
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER"),
                "github"
            );
            
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authToken);
        
        jakarta.servlet.http.HttpSession session = request.getSession(true);
        session.setAttribute(
            org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            org.springframework.security.core.context.SecurityContextHolder.getContext()
        );
        
        return ResponseEntity.ok(java.util.Map.of(
            "authenticated", true, 
            "login", "gitpilot-mock-user",
            "name", "GitPilot Mock User"
        ));
    }
}
