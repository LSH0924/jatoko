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

package com.jatoko.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SVG 파일 내의 텍스트 노드를 나타내는 모델
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SvgTextNode {

    /**
     * 텍스트 요소의 고유 ID (elementId 속성 또는 자동 생성)
     */
    private String id;

    /**
     * 원본 텍스트 (일본어)
     */
    private String originalText;

    /**
     * 번역된 텍스트 (한국어)
     */
    private String translatedText;

    /**
     * 텍스트의 x 좌표
     */
    private String x;

    /**
     * 텍스트의 y 좌표
     */
    private String y;

    /**
     * 폰트 크기 (선택적)
     */
    private String fontSize;

    /**
     * 폰트 패밀리 (선택적)
     */
    private String fontFamily;
}
