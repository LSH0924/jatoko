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

package com.jatoko.service.extractor;

import com.jatoko.model.SvgTextNode;
import com.jatoko.util.JapaneseDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SVG 문서에서 일본어 텍스트를 추출하는 클래스
 * <text> 요소와 <foreignObject> 내부의 텍스트를 모두 처리합니다.
 */
@Slf4j
@Component
public class SvgTextExtractor {

    /**
     * SVG 문서에서 일본어 텍스트 노드를 추출합니다.
     *
     * @param document SVG Document 객체
     * @return 일본어 텍스트 노드 리스트
     */
    public List<SvgTextNode> extractJapaneseTexts(Document document) {
        List<SvgTextNode> japaneseTexts = new ArrayList<>();

        // 1. <text> 요소에서 추출
        japaneseTexts.addAll(extractFromTextElements(document));

        // 2. <foreignObject> 요소에서 추출
        japaneseTexts.addAll(extractFromForeignObjects(document));

        log.info("SVG 문서에서 {}개의 일본어 텍스트 노드 추출 완료", japaneseTexts.size());
        return japaneseTexts;
    }

    /**
     * <text> 요소에서 일본어 텍스트를 추출합니다.
     *
     * @param document SVG Document 객체
     * @return 일본어 텍스트 노드 리스트
     */
    private List<SvgTextNode> extractFromTextElements(Document document) {
        List<SvgTextNode> nodes = new ArrayList<>();
        NodeList textElements = document.getElementsByTagName("text");

        for (int i = 0; i < textElements.getLength(); i++) {
            Element textElement = (Element) textElements.item(i);
            String textContent = extractTextContent(textElement);

            // 일본어 포함 여부 확인
            if (!textContent.trim().isEmpty() && JapaneseDetector.containsJapanese(textContent)) {
                // ID 생성 또는 가져오기
                String id = textElement.getAttribute("id");
                if (id.isEmpty()) {
                    id = "text_" + UUID.randomUUID().toString();
                    textElement.setAttribute("id", id);
                }

                SvgTextNode node = new SvgTextNode();
                node.setId(id);
                node.setOriginalText(textContent);
                node.setX(textElement.getAttribute("x"));
                node.setY(textElement.getAttribute("y"));
                node.setFontSize(textElement.getAttribute("font-size"));
                node.setFontFamily(textElement.getAttribute("font-family"));

                nodes.add(node);
                log.debug("일본어 텍스트 발견 (text): id={}, text={}", id, textContent);
            }
        }

        return nodes;
    }

    /**
     * <foreignObject> 요소에서 일본어 텍스트를 추출합니다.
     *
     * @param document SVG Document 객체
     * @return 일본어 텍스트 노드 리스트
     */
    private List<SvgTextNode> extractFromForeignObjects(Document document) {
        List<SvgTextNode> nodes = new ArrayList<>();
        NodeList foreignObjects = document.getElementsByTagName("foreignObject");

        for (int i = 0; i < foreignObjects.getLength(); i++) {
            Element foreignObject = (Element) foreignObjects.item(i);
            String textContent = extractForeignObjectText(foreignObject);

            // 일본어 포함 여부 확인
            if (!textContent.trim().isEmpty() && JapaneseDetector.containsJapanese(textContent)) {
                // foreignObject의 부모 g 요소에서 ID 가져오기 또는 생성
                Element parentGroup = (Element) foreignObject.getParentNode();
                String id = parentGroup.getAttribute("id");
                if (id.isEmpty()) {
                    id = "foreign_" + UUID.randomUUID();
                    parentGroup.setAttribute("id", id);
                }

                SvgTextNode node = new SvgTextNode();
                node.setId(id);
                node.setOriginalText(textContent);

                nodes.add(node);
                log.debug("일본어 텍스트 발견 (foreignObject): id={}, text={}", id, textContent);
            }
        }

        return nodes;
    }

    /**
     * <text> 요소의 텍스트 내용을 추출합니다.
     * <tspan> 등의 자식 요소도 고려합니다.
     *
     * @param textElement <text> 요소
     * @return 추출된 텍스트 내용
     */
    public String extractTextContent(Element textElement) {
        StringBuilder content = new StringBuilder();
        NodeList children = textElement.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                content.append(child.getTextContent());
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                if ("tspan".equals(child.getNodeName())) {
                    content.append(child.getTextContent());
                }
            }
        }

        return content.toString().trim();
    }

    /**
     * <foreignObject> 내부에서 텍스트를 추출합니다.
     * HTML div/span 구조에서 text-edit 클래스의 span을 찾습니다.
     *
     * @param foreignObject <foreignObject> 요소
     * @return 추출된 텍스트 내용
     */
    public String extractForeignObjectText(Element foreignObject) {
        // foreignObject → div → div → div → span.text-edit 구조 탐색
        NodeList children = foreignObject.getElementsByTagName("span");
        for (int i = 0; i < children.getLength(); i++) {
            Element span = (Element) children.item(i);
            String className = span.getAttribute("class");
            if (className.contains("text-edit")) {
                return span.getTextContent().trim();
            }
        }
        return "";
    }
}
