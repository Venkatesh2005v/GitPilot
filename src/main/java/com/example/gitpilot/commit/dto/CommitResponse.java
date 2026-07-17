package com.example.gitpilot.commit.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommitResponse {
    private String sha;
    private String message;
    private String authorName;
    private String authorEmail;
    private LocalDateTime commitDate;
    private String commitUrl;
}
