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
