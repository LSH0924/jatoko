package com.jatoko.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileMetadataDto {
    private String fileName;
    private boolean translated;
    private LocalDateTime uploadedAt;
    private LocalDateTime translatedAt;
}
