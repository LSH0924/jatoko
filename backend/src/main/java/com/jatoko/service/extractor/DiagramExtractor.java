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

import com.change_vision.jude.api.inf.model.IDiagram;
import com.jatoko.model.DiagramNode;

import java.util.List;
import java.util.Map;

/**
 * 다이어그램에서 일본어 요소를 추출하고 번역을 적용하는 인터페이스
 */
public interface DiagramExtractor {

    /**
     * 다이어그램에서 일본어 요소를 추출합니다.
     *
     * @param diagram 대상 다이어그램
     * @param japaneseNodes 추출된 노드를 추가할 리스트
     */
    void extract(IDiagram diagram, List<DiagramNode> japaneseNodes);

    /**
     * 다이어그램에 번역을 적용합니다.
     *
     * @param diagram 대상 다이어그램
     * @param translationMap 번역 맵
     * @return 적용된 번역 개수
     */
    int applyTranslations(IDiagram diagram, Map<String, String> translationMap);

    /**
     * 이 추출기가 처리할 수 있는 다이어그램 타입인지 확인합니다.
     *
     * @param diagram 확인할 다이어그램
     * @return 처리 가능 여부
     */
    boolean supports(IDiagram diagram);
}
