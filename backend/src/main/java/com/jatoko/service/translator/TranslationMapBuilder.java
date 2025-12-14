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

package com.jatoko.service.translator;

import com.jatoko.model.DiagramNode;
import com.jatoko.service.extractor.DiagramExtractor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 번역 노드를 타입별 맵으로 변환하는 컴포넌트
 */
@Component
public class TranslationMapBuilder {

    private final List<DiagramExtractor> extractors;

    public TranslationMapBuilder(List<DiagramExtractor> extractors) {
        this.extractors = extractors;
    }

    /**
     * 타입별 번역 맵을 생성합니다.
     *
     * @param nodes 번역된 노드 리스트
     * @param progressCallback 진행 상황 콜백 (optional)
     * @return 타입별 번역 맵 (키: 타입, 값: ID → translatedName 맵)
     */
    public Map<String, Map<String, String>> buildTranslationMaps(List<DiagramNode> nodes,
                                                                   Consumer<Integer> progressCallback) {
        Map<String, Map<String, String>> maps = new HashMap<>();
        maps.put("model", new HashMap<>());
        maps.put("diagram", new HashMap<>());
        maps.put("comment", new HashMap<>());

        // 다이어그램 타입별 맵
        for (DiagramExtractor extractor : extractors) {
            maps.put(extractor.getClass().getSimpleName(), new HashMap<>());
        }

        int totalNodes = nodes.size();
        int processedNodes = 0;
        final int BATCH_SIZE = 1000;

        // 타입별 카운터 (디버깅용)
        Map<String, Integer> typeCounters = new HashMap<>();

        for (DiagramNode node : nodes) {
            if (node.getTranslatedName() != null && !node.getTranslatedName().isEmpty()) {
                String type = node.getType();
                String translatedName = node.getTranslatedName();

                // 현재 노드를 맵에 추가
                addNodeToMap(maps, typeCounters, node.getId(), node.getDiagramId(), type, translatedName);

                // 중복 노드들도 동일한 번역으로 맵에 추가
                if (node.getDuplicateNodeIds() != null && !node.getDuplicateNodeIds().isEmpty()) {
                    for (String duplicateId : node.getDuplicateNodeIds()) {
                        addNodeToMap(maps, typeCounters, duplicateId, null, type, translatedName);
                    }
                }
            }

            processedNodes++;
            if (progressCallback != null && processedNodes % BATCH_SIZE == 0) {
                int progress = (int) ((processedNodes * 30.0) / totalNodes);
                progressCallback.accept(progress);
            }
        }

        // 타입별 분포 로깅
        System.out.println("=== 번역 맵 생성 완료 ===");
        typeCounters.forEach((key, value) ->
            System.out.println("  " + key + ": " + value + "개"));
        System.out.println("======================");

        return maps;
    }

    /**
     * 간단한 번역 맵 생성 (ID → translatedName)
     *
     * @param nodes 번역된 노드 리스트
     * @return ID → translatedName 맵
     */
    public Map<String, String> buildSimpleTranslationMap(List<DiagramNode> nodes) {
        Map<String, String> translationMap = new HashMap<>();
        for (DiagramNode node : nodes) {
            if (node.getTranslatedName() != null && !node.getTranslatedName().isEmpty()) {
                translationMap.put(node.getId(), node.getTranslatedName());

                // 중복 노드들도 동일한 번역으로 맵에 추가
                if (node.getDuplicateNodeIds() != null && !node.getDuplicateNodeIds().isEmpty()) {
                    for (String duplicateId : node.getDuplicateNodeIds()) {
                        translationMap.put(duplicateId, node.getTranslatedName());
                    }
                }
            }
        }
        return translationMap;
    }

    /**
     * 노드를 타입에 맞는 번역 맵에 추가하는 헬퍼 메서드
     */
    private void addNodeToMap(Map<String, Map<String, String>> maps,
                              Map<String, Integer> typeCounters,
                              String nodeId,
                              String diagramId,
                              String type,
                              String translatedName) {
        if ("diagram_name".equals(type)) {
            if (diagramId != null) {
                maps.get("diagram").put(diagramId, translatedName);
                typeCounters.merge("diagram", 1, Integer::sum);
            }
        } else if ("comment_body".equals(type)) {
            maps.get("comment").put(nodeId, translatedName);
            typeCounters.merge("comment", 1, Integer::sum);
        } else if (type != null) {
            // 타입별로 적절한 맵에 할당
            if (type.startsWith("er_")) {
                maps.get("ERDiagramExtractor").put(nodeId, translatedName);
                typeCounters.merge("ERDiagramExtractor", 1, Integer::sum);
            } else if (type.startsWith("sequence_")) {
                maps.get("SequenceDiagramExtractor").put(nodeId, translatedName);
                typeCounters.merge("SequenceDiagramExtractor", 1, Integer::sum);
            } else if (type.equals("mindmap_topic")) {
                maps.get("MindMapExtractor").put(nodeId, translatedName);
                typeCounters.merge("MindMapExtractor", 1, Integer::sum);
            } else if (type.startsWith("activity_")) {
                maps.get("ActivityDiagramExtractor").put(nodeId, translatedName);
                typeCounters.merge("ActivityDiagramExtractor", 1, Integer::sum);
            } else if (type.startsWith("presentation_")) {
                maps.get("GenericDiagramExtractor").put(nodeId, translatedName);
                typeCounters.merge("GenericDiagramExtractor", 1, Integer::sum);
            } else {
                maps.get("model").put(nodeId, translatedName);
                typeCounters.merge("model", 1, Integer::sum);
            }
        }
    }
}
