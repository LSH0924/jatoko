/*
 * JaToKo (Japanese-to-Korean Translator)
 * Copyright (C) 2025 The JaToKo Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
    private Integer originalVersion;  // 원본 버전 (동일 baseName 원본 파일 개수)
}
