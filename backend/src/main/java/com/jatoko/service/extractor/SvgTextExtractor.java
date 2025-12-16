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

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
     * 내부의 각 텍스트 요소를 개별적으로 추출합니다.
     *
     * @param document SVG Document 객체
     * @return 일본어 텍스트 노드 리스트
     */
    private List<SvgTextNode> extractFromForeignObjects(Document document) {
        List<SvgTextNode> nodes = new ArrayList<>();
        NodeList foreignObjects = document.getElementsByTagName("foreignObject");

        for (int i = 0; i < foreignObjects.getLength(); i++) {
            Element foreignObject = (Element) foreignObjects.item(i);
            // foreignObject 내부의 각 텍스트 요소를 개별 추출
            extractTextElementsRecursively(foreignObject, nodes);
        }

        return nodes;
    }

    /**
     * foreignObject 내부를 재귀적으로 탐색하여 텍스트가 있는 요소를 개별 추출합니다.
     *
     * @param node 탐색할 노드
     * @param nodes 추출된 노드를 저장할 리스트
     */
    private void extractTextElementsRecursively(Node node, List<SvgTextNode> nodes) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;
                String directText = getDirectTextContent(element);

                // 직접 텍스트가 있고 일본어를 포함하면 추출
                if (!directText.isEmpty() && JapaneseDetector.containsJapanese(directText)) {
                    String id = "foreign_" + hashElement(element);

                    SvgTextNode svgNode = new SvgTextNode();
                    svgNode.setId(id);
                    svgNode.setOriginalText(directText);

                    nodes.add(svgNode);
                    log.debug("일본어 텍스트 발견 (foreignObject 내부): id={}, text={}", id, directText);
                }

                // 자식 요소도 재귀 탐색
                extractTextElementsRecursively(element, nodes);
            }
        }
    }

    /**
     * 요소의 직접 텍스트 노드 내용만 추출합니다 (자식 요소의 텍스트 제외).
     *
     * @param element 대상 요소
     * @return 직접 텍스트 내용
     */
    private String getDirectTextContent(Element element) {
        StringBuilder content = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                String text = child.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    if (content.length() > 0) {
                        content.append(" ");
                    }
                    content.append(text.trim());
                }
            }
        }
        return content.toString();
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
     * 모든 자식 노드를 재귀적으로 탐색하여 텍스트를 수집합니다.
     * (h1, p, div, span, textarea 등 모든 HTML 요소 지원)
     *
     * @param foreignObject <foreignObject> 요소
     * @return 추출된 텍스트 내용
     */
    public String extractForeignObjectText(Element foreignObject) {
        StringBuilder content = new StringBuilder();
        extractTextRecursively(foreignObject, content);
        return content.toString().trim();
    }

    /**
     * 노드의 모든 자식을 재귀적으로 탐색하여 텍스트를 추출합니다.
     *
     * @param node 탐색할 노드
     * @param content 텍스트를 수집할 StringBuilder
     */
    private void extractTextRecursively(Node node, StringBuilder content) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                String text = child.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    if (content.length() > 0) {
                        content.append(" ");
                    }
                    content.append(text.trim());
                }
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                extractTextRecursively(child, content);
            }
        }
    }

    /**
     * 요소의 outerHTML을 SHA-256으로 해시하여 앞 10자리를 반환합니다.
     *
     * @param element 해시할 요소
     * @return 해시값 앞 10자리
     */
    public String hashElement(Element element) {
        try {
            StringWriter writer = new StringWriter();
            TransformerFactory.newInstance()
                    .newTransformer()
                    .transform(new DOMSource(element), new StreamResult(writer));
            String outerHtml = writer.toString();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(outerHtml.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 10);
        } catch (Exception e) {
            log.warn("요소 해시 생성 실패, UUID 사용: {}", e.getMessage());
            return UUID.randomUUID().toString().substring(0, 10);
        }
    }
}
