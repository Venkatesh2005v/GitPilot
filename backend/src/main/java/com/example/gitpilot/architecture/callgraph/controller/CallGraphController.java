package com.example.gitpilot.architecture.callgraph.controller;

import com.example.gitpilot.architecture.callgraph.dto.CallGraphResponseDto;
import com.example.gitpilot.architecture.callgraph.service.CallGraphService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/architecture/call-graph")
@RequiredArgsConstructor
public class CallGraphController {

    private final CallGraphService callGraphService;

    @GetMapping("/{repositoryId}")
    public ResponseEntity<CallGraphResponseDto> getCallGraph(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String rootKey) {
        CallGraphResponseDto dto = callGraphService.generateCallGraph(repositoryId, rootKey);
        return ResponseEntity.ok(dto);
    }
}
