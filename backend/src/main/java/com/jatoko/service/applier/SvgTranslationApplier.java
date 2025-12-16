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

package com.jatoko.service.applier;

import com.jatoko.service.svg.SvgStyleManager;
import com.jatoko.service.extractor.SvgTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map;

/**
 * SVG 문서에 번역을 적용하는 클래스
 * <text> 요소와 <foreignObject> 내부의 텍스트에 번역을 적용합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SvgTranslationApplier {

    private final SvgTextExtractor textExtractor;
    private final SvgStyleManager styleManager;

    /**
     * SVG 문서에 번역을 적용합니다.
     *
     * @param document SVG Document 객체
     * @param translations 번역 맵 (id -> translatedText)
     * @return 적용된 번역 개수
     */
    public int applyTranslations(Document document, Map<String, String> translations) {
        // CSS 스타일 추가
        styleManager.addOverlayStyles(document);

        int appliedCount = 0;

        // 1. <text> 요소 처리 (CSS 오버레이 구조로 변환)
        appliedCount += applyToTextElements(document, translations);

        // 2. <foreignObject> 처리
        appliedCount += applyToForeignObjects(document, translations);

        log.info("번역 적용 완료: {}개 텍스트 노드", appliedCount);
        return appliedCount;
    }

    /**
     * <text> 요소에 번역을 적용합니다.
     *
     * @param document SVG Document 객체
     * @param translations 번역 맵
     * @return 적용된 번역 개수
     */
    private int applyToTextElements(Document document, Map<String, String> translations) {
        int count = 0;
        NodeList textElements = document.getElementsByTagName("text");

        for (int i = 0; i < textElements.getLength(); i++) {
            Element textElement = (Element) textElements.item(i);
            String id = textElement.getAttribute("id");

            if (translations.containsKey(id)) {
                String originalText = textExtractor.extractTextContent(textElement);
                String translatedText = translations.get(id);
                wrapTextWithOverlay(document, textElement, originalText, translatedText);
                count++;
                log.debug("번역 오버레이 적용 (text): id={}, original={}, translation={}",
                          id, originalText, translatedText);
            }
        }

        return count;
    }

    /**
     * <foreignObject> 요소에 번역을 적용합니다.
     * 내부의 각 텍스트 요소에 개별적으로 번역을 적용합니다.
     *
     * @param document SVG Document 객체
     * @param translations 번역 맵
     * @return 적용된 번역 개수
     */
    private int applyToForeignObjects(Document document, Map<String, String> translations) {
        int count = 0;
        NodeList foreignObjects = document.getElementsByTagName("foreignObject");

        for (int i = 0; i < foreignObjects.getLength(); i++) {
            Element foreignObject = (Element) foreignObjects.item(i);
            // foreignObject 내부의 각 텍스트 요소에 번역 적용
            count += applyToTextElementsRecursively(foreignObject, translations);
        }

        return count;
    }

    /**
     * foreignObject 내부를 재귀적으로 탐색하여 각 텍스트 요소에 번역을 적용합니다.
     *
     * @param node 탐색할 노드
     * @param translations 번역 맵
     * @return 적용된 번역 개수
     */
    private int applyToTextElementsRecursively(Node node, Map<String, String> translations) {
        int count = 0;
        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;
                // 요소의 해시로 id 생성 (추출 시와 동일한 방식)
                String id = "foreign_" + textExtractor.hashElement(element);

                if (translations.containsKey(id)) {
                    String translatedText = translations.get(id);
                    applyOverlayToElement(element, translatedText);
                    count++;
                    log.debug("번역 적용 (foreignObject 내부): id={}, text={}", id, translatedText);
                }

                // 자식 요소도 재귀 탐색
                count += applyToTextElementsRecursively(element, translations);
            }
        }

        return count;
    }

    /**
     * 특정 텍스트 요소에 오버레이를 적용합니다.
     *
     * @param element 대상 요소
     * @param translatedText 번역된 텍스트
     */
    private void applyOverlayToElement(Element element, String translatedText) {
        Document document = element.getOwnerDocument();
        String className = element.getAttribute("class");

        // jp-translated 클래스 추가
        if (className.isEmpty()) {
            element.setAttribute("class", "jp-translated");
        } else {
            element.setAttribute("class", className + " jp-translated");
        }

        // 부모의 부모 div에서 white-space: pre-wrap 제거
        removeWhiteSpacePreWrap(element);

        // 원본 텍스트의 font-size 추출
        String fontSize = extractFontSize(element);

        // 번역 오버레이 span 생성 (CSS로 숨김/표시)
        Element overlaySpan = document.createElement("span");
        overlaySpan.setAttribute("class", "jp-overlay");

        // 원본보다 작은 font-size 적용
        if (!fontSize.isEmpty()) {
            String smallerFontSize = reduceFontSize(fontSize);
            String style = overlaySpan.getAttribute("style");
            overlaySpan.setAttribute("style", style + "font-size: " + smallerFontSize + ";");
        }

        overlaySpan.setTextContent(translatedText);

        // jp-wrapper 생성 (블록 요소는 div로 감싸기.) TODO 스타일 보존 미비함
        Element wrapper = document.createElement("div");
        wrapper.setAttribute("class", "jp-wrapper");

        // 원문 요소를 wrapper로 감싸기
        Node parentNode = element.getParentNode();
        parentNode.replaceChild(wrapper, element);
        wrapper.appendChild(element);
        wrapper.appendChild(overlaySpan);

        log.debug("요소에 오버레이 적용: translation={}, fontSize={}", translatedText, fontSize);
    }

    /**
     * <text> 요소를 오버레이 구조로 변환합니다.
     * 원문과 번역을 같은 위치에 배치하고, 호버 시 원문이 사라지고 번역이 덧씌워집니다.
     *
     * @param document SVG 문서
     * @param textElement 변환할 <text> 요소
     * @param originalText 일본어 원문
     * @param translatedText 한국어 번역
     */
    private void wrapTextWithOverlay(Document document, Element textElement,
                                      String originalText, String translatedText) {
        // 기존 <text>의 부모 노드와 속성 가져오기
        Node parent = textElement.getParentNode();

        // 속성 추출
        TextElementAttributes attrs = extractTextAttributes(textElement);

        // <g class="jp-text-wrapper"> 생성
        Element groupElement = document.createElement("g");
        groupElement.setAttribute("class", "jp-text-wrapper");

        // 원문 <text> (호버 시 투명해짐)
        Element originalTextElement = createTextElement(document, "original-text", attrs, originalText);

        // 번역 <text> (기본 숨김, 호버 시 표시)
        Element tooltipTextElement = createTextElement(document, "tooltip-text", attrs, translatedText);

        // 구조 조립: group에 원문 + 번역 추가
        groupElement.appendChild(originalTextElement);
        groupElement.appendChild(tooltipTextElement);

        // 기존 <text>를 <g>로 교체
        parent.replaceChild(groupElement, textElement);

        log.debug("텍스트 요소를 오버레이 구조로 변환: original={}, translation={}", originalText, translatedText);
    }

    /**
     * 요소의 font-size를 추출합니다.
     * style 속성, CSS 상속, 또는 부모 요소에서 font-size를 찾습니다.
     *
     * @param element 대상 요소
     * @return font-size 값 (예: "12px", "14pt") 또는 빈 문자열
     */
    private String extractFontSize(Element element) {
        // 1. 요소 자체의 style 속성에서 font-size 찾기
        String style = element.getAttribute("style");
        if (style != null && !style.isEmpty()) {
            String fontSize = extractFontSizeFromStyle(style);
            if (!fontSize.isEmpty()) {
                return fontSize;
            }
        }

        // 2. 부모 요소 탐색 (최대 3단계)
        Node parent = element.getParentNode();
        for (int i = 0; i < 3 && parent instanceof Element parentElement; i++) {
            String parentStyle = parentElement.getAttribute("style");
            if (parentStyle != null && !parentStyle.isEmpty()) {
                String fontSize = extractFontSizeFromStyle(parentStyle);
                if (!fontSize.isEmpty()) {
                    return fontSize;
                }
            }
            parent = parentElement.getParentNode();
        }

        return ""; // font-size를 찾지 못한 경우
    }

    /**
     * font-size 값을 지정된 픽셀만큼 줄입니다.
     *
     * @param fontSize 원본 font-size (예: "12px", "14pt")
     * @return 줄어든 font-size (예: "11px") 또는 원본 값
     */
    private String reduceFontSize(String fontSize) {
        int reduceBy = 1;
        // px 단위인 경우만 처리
        if (fontSize.endsWith("px")) {
            try {
                String numPart = fontSize.replace("px", "").trim();
                double fontSizeDouble = Double.parseDouble(numPart);
                if (fontSizeDouble >= 15.0) {
                    reduceBy = 2;
                } else if (fontSizeDouble <= 10.0) {
                    reduceBy = 0;
                }
                double reduced = Math.max(1, fontSizeDouble - reduceBy);
                return reduced + "px";
            } catch (NumberFormatException e) {
                return fontSize;
            }
        }
        // pt 단위인 경우
        if (fontSize.endsWith("pt")) {
            try {
                String numPart = fontSize.replace("pt", "").trim();
                double fontSizeDouble = Double.parseDouble(numPart);
                if (fontSizeDouble >= 15.0) {
                    reduceBy = 2;
                } else if (fontSizeDouble < 10.0) {
                    reduceBy = 0;
                }
                double reduced = Math.max(1, fontSizeDouble - reduceBy);
                return reduced + "pt";
            } catch (NumberFormatException e) {
                return fontSize;
            }
        }
        return fontSize;
    }

    /**
     * style 속성 문자열에서 font-size 값을 추출합니다.
     *
     * @param style CSS style 문자열 (예: "font-size: 12px; color: red;")
     * @return font-size 값 (예: "12px") 또는 빈 문자열
     */
    private String extractFontSizeFromStyle(String style) {
        // "font-size: 12px" 또는 "font-size:12px" 패턴 매칭
        String[] parts = style.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("font-size:") || trimmed.startsWith("font-size :")) {
                String fontSize = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                if (!fontSize.isEmpty()) {
                    return fontSize;
                }
            }
        }
        return "";
    }

    /**
     * span의 부모의 부모 div에서 white-space: pre-wrap 스타일을 제거합니다.
     * 번역 오버레이가 정상적으로 표시되도록 레이아웃을 조정합니다.
     */
    private void removeWhiteSpacePreWrap(Element span) {
        Node parent = span.getParentNode();
        if (parent instanceof Element parentDiv) {
            Node grandParent = parentDiv.getParentNode();
            if (grandParent instanceof Element grandParentDiv) {
                String style = grandParentDiv.getAttribute("style");
                if (style != null && style.contains("white-space: pre-wrap")) {
                    String updatedStyle = style.replace("white-space: pre-wrap", "")
                                               .replaceAll(";\\s*;", ";")  // 연속된 세미콜론 제거
                                               .replaceAll("^\\s*;|;\\s*$", "")  // 앞뒤 세미콜론 제거
                                               .trim();
                    grandParentDiv.setAttribute("style", updatedStyle);
                    log.debug("부모의 부모 div에서 white-space: pre-wrap 제거");
                }
            }
        }
    }

    /**
     * <text> 요소의 속성을 추출합니다.
     */
    private TextElementAttributes extractTextAttributes(Element textElement) {
        return new TextElementAttributes(
            textElement.getAttribute("x"),
            textElement.getAttribute("y"),
            textElement.getAttribute("font-size"),
            textElement.getAttribute("font-family"),
            textElement.getAttribute("fill"),
            textElement.getAttribute("text-anchor")
        );
    }

    /**
     * <text> 요소를 생성합니다.
     */
    private Element createTextElement(Document document, String className,
                                       TextElementAttributes attrs, String textContent) {
        Element textElement = document.createElement("text");
        textElement.setAttribute("class", className);
        textElement.setAttribute("x", attrs.x);
        textElement.setAttribute("y", attrs.y);

        if (!attrs.fontSize.isEmpty()) textElement.setAttribute("font-size", attrs.fontSize);
        if (!attrs.fontFamily.isEmpty()) textElement.setAttribute("font-family", attrs.fontFamily);
        if (!attrs.fill.isEmpty()) textElement.setAttribute("fill", attrs.fill);
        if (!attrs.textAnchor.isEmpty()) textElement.setAttribute("text-anchor", attrs.textAnchor);

        textElement.setTextContent(textContent);
        return textElement;
    }


    /**
     * <text> 요소의 속성을 담는 내부 클래스
     */
    private record TextElementAttributes(
        String x,
        String y,
        String fontSize,
        String fontFamily,
        String fill,
        String textAnchor
    ) {}
}
