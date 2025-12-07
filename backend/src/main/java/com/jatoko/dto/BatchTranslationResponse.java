package com.jatoko.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchTranslationResponse {
    private List<String> successFiles;
    private List<String> failedFiles;
    private int totalCount;
    private int successCount;
    private int failedCount;
}
