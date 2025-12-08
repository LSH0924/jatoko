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
    private boolean outlined;  // SVG 아웃라인 여부 (텍스트 추출 불가)
    private Integer version;   // 번역 버전 (번역 파일 개수, null이면 번역 없음)
}
