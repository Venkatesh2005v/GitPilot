package com.example.gitpilot.architecture.apiflow.controller;

import com.example.gitpilot.architecture.apiflow.dto.ApiFlowPathDto;
import com.example.gitpilot.architecture.apiflow.service.ApiFlowService;
import com.example.gitpilot.security.GitHubTokenResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/architecture/api-flows")
@RequiredArgsConstructor
public class ApiFlowController {

    private final ApiFlowService apiFlowService;
    private final GitHubTokenResolver tokenResolver;

    @GetMapping("/{repositoryId}")
    public ResponseEntity<List<ApiFlowPathDto>> getApiFlows(
            @PathVariable Long repositoryId, Authentication authentication, HttpServletRequest request) {
        String accessToken = tokenResolver.resolve(authentication, request);
        List<ApiFlowPathDto> flows = apiFlowService.discoverApiFlows(repositoryId, accessToken);
        return ResponseEntity.ok(flows);
    }
}
