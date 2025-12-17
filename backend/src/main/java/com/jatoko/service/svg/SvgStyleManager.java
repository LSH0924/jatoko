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

package com.jatoko.service.svg;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SVG 문서의 CSS 스타일을 관리하는 클래스
 * 번역 오버레이를 위한 CSS 스타일을 추가합니다.
 */
@Slf4j
@Component
public class SvgStyleManager {

    /**
     * SVG 문서에 CSS 오버레이 스타일을 추가합니다.
     * 호버 시 번역 텍스트가 원문 위에 완전히 덧씌워져 표시됩니다.
     *
     * @param document SVG 문서
     */
    public void addOverlayStyles(Document document) {
        Element svgRoot = document.getDocumentElement();
        Element defsElement = document.createElement("defs");
        Element styleElement = document.createElement("style");
        styleElement.setAttribute("type", "text/css");

        String cssContent =
            /* SVG <text> 요소 오버레이 */
            ".jp-text-wrapper { cursor: pointer; position: relative; }\n" +
            ".tooltip-text {\n" +
            "  opacity: 0;\n" +
            "  pointer-events: none;\n" +
            "  transition: opacity 0.3s ease;\n" +
            "}\n" +
            ".jp-text-wrapper:hover .original-text { opacity: 0; }\n" +
            ".jp-text-wrapper:hover .tooltip-text {\n" +
            "  opacity: 1;\n" +
            "  fill: #2563eb;\n" +
            "  font-weight: bold;\n" +
            "}\n" +
            /* foreignObject 내부 HTML 요소 오버레이 (원문 span 크기로 hover 영역 제한) */
            ".jp-wrapper {\n" +
            "  position: relative;\n" +
            "  display: inline-block;\n" +
            "  cursor: pointer;\n" +
            "}\n" +
            ".jp-wrapper .jp-overlay {\n" +
            "  position: absolute;\n" +
            "  top: -4px;\n" +
            "  left: -2px;\n" +
            "  width: calc(100% + 6px);\n" +
            "  height: calc(100% + 8px);\n" +
            "  display: flex;\n" +
            "  align-items: center;\n" +
            "  justify-content: center;\n" +
            "  line-height: 1.3;\n" +
            "  background-color: rgba(37, 99, 235, 0.95);\n" +
            "  color: white;\n" +
            "  font-weight: bold;\n" +
            "  opacity: 0;\n" +
            "  pointer-events: none;\n" +
            "  transition: opacity 0.3s ease;\n" +
            "  z-index: 1000;\n" +
            "}\n" +
            ".jp-wrapper:hover .jp-overlay {\n" +
            "  opacity: 1;\n" +
            "}";

        styleElement.setTextContent(cssContent);
        defsElement.appendChild(styleElement);

        // 클립보드 복사 JavaScript 추가
        Element scriptElement = document.createElement("script");
        scriptElement.setAttribute("type", "text/javascript");
        String jsContent =
            "function copyOriginalText(event) {\n" +
            "  var originalText = event.currentTarget.getAttribute('data-original');\n" +
            "  if (originalText && navigator.clipboard) {\n" +
            "    navigator.clipboard.writeText(originalText);\n" +
            "    alert('원문 일본어가 복사되었습니다.');\n" +
            "  }\n" +
            "}";
        scriptElement.setTextContent(jsContent);
        defsElement.appendChild(scriptElement);

        svgRoot.insertBefore(defsElement, svgRoot.getFirstChild());

        log.debug("CSS 오버레이 스타일 및 클릭 핸들러 추가 완료 (SVG text + foreignObject)");
    }
}
