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
            Element parentGroup = (Element) foreignObject.getParentNode();
            String id = parentGroup.getAttribute("id");

            if (translations.containsKey(id)) {
                String translatedText = translations.get(id);
                applyToForeignObjectText(foreignObject, translatedText);
                count++;
                log.debug("번역 적용 (foreignObject): id={}, text={}", id, translatedText);
            }
        }

        return count;
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
     * <foreignObject> 내부에 CSS 기반 오버레이를 추가합니다.
     * 기존 text-edit span에 jp-translated 클래스를 추가하고 오버레이를 직접 삽입합니다.
     * 호버 시 번역이 원문 위에 완전히 덧씌워져 표시됩니다.
     */
    private void applyToForeignObjectText(Element foreignObject, String translatedText) {
        // foreignObject → div → div → div → span.text-edit 구조 탐색
        NodeList children = foreignObject.getElementsByTagName("span");
        for (int i = 0; i < children.getLength(); i++) {
            Element span = (Element) children.item(i);
            String className = span.getAttribute("class");
            if (className.contains("text-edit")) {
                Document document = span.getOwnerDocument();
                String originalText = span.getTextContent();

                // 기존 text-edit span에 jp-translated 클래스 추가
                span.setAttribute("class", className + " jp-translated");

                // span의 부모의 부모 div에서 white-space: pre-wrap 제거
                removeWhiteSpacePreWrap(span);

                // 원본 텍스트의 font-size 추출
                String fontSize = extractFontSize(span);

                // 번역 오버레이 span 생성 (CSS로 숨김/표시)
                Element overlaySpan = document.createElement("span");
                overlaySpan.setAttribute("class", "translation-overlay");

                // 원본과 동일한 font-size 적용
                if (!fontSize.isEmpty()) {
                    String style = overlaySpan.getAttribute("style");
                    overlaySpan.setAttribute("style", style + "font-size: " + fontSize + ";");
                }

                overlaySpan.setTextContent(translatedText);

                // 기존 span에 오버레이 직접 추가
                span.appendChild(overlaySpan);

                log.debug("foreignObject에 CSS 오버레이 추가: original={}, translation={}, fontSize={}",
                          originalText, translatedText, fontSize);
                break; // 첫 번째 text-edit span만 수정
            }
        }
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
