package com.example.gitpilot.commit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single file changed within a commit. Not persisted — served on demand from GitHub.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChangedFileDto {
    private String filename;
    private String status;            // added | modified | removed | renamed | ...
    private int additions;
    private int deletions;
    private int changes;
    private String previousFilename;  // nullable; set only for renames
    private String patch;             // nullable; null for binary/omitted diffs
    private boolean patchTruncated;   // true when a very large patch was truncated server-side
}
