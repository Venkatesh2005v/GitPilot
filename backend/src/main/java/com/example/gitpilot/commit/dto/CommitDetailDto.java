package com.example.gitpilot.commit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * On-demand detail for a single commit. Not persisted.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommitDetailDto {
    private String sha;
    private String message;
    private String authorName;
    private String authorEmail;
    private LocalDateTime date;
    private int additions;
    private int deletions;
    private int changedFileCount;
    private String htmlUrl;
    private List<ChangedFileDto> files;
}
