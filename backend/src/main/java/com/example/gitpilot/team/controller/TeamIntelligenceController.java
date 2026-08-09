package com.example.gitpilot.team.controller;

import com.example.gitpilot.team.dto.BusFactorReportDto;
import com.example.gitpilot.team.service.TeamIntelligenceService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamIntelligenceController {

    private final TeamIntelligenceService teamIntelligenceService;

    @GetMapping("/bus-factor/{repositoryId}")
    public ResponseEntity<BusFactorReportDto> getBusFactorReport(@PathVariable Long repositoryId) {
        BusFactorReportDto report = teamIntelligenceService.computeTeamIntelligence(repositoryId);
        return ResponseEntity.ok(report);
    }
}
