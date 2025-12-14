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

import com.change_vision.jude.api.inf.model.*;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.jatoko.model.DiagramNode;
import com.jatoko.util.JapaneseDetector;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * ER 다이어그램 추출기
 */
@Component
@Order(1)
public class ERDiagramExtractor extends BaseExtractor implements DiagramExtractor {

    @Override
    public boolean supports(IDiagram diagram) {
        return diagram instanceof IERDiagram;
    }

    @Override
    public void extract(IDiagram diagram, List<DiagramNode> japaneseNodes) {
        if (!(diagram instanceof IERDiagram)) {
            return;
        }

        IERDiagram erDiagram = (IERDiagram) diagram;

        try {
            IPresentation[] presentations = erDiagram.getPresentations();
            for (IPresentation presentation : presentations) {
                try {
                    if (presentation.getModel() instanceof IEREntity) {
                        IEREntity entity = (IEREntity) presentation.getModel();
                        extractFromEREntity(entity, japaneseNodes);
                    }
                    extractFromPresentation(presentation, japaneseNodes);
                } catch (Exception e) {
                    // 일부 프레젠테이션 처리 실패 시 계속 진행
                }
            }
        } catch (Exception e) {
            System.err.println("ER 다이어그램 추출 실패: " + e.getMessage());
        }
    }

    @Override
    public int applyTranslations(IDiagram diagram, Map<String, String> translationMap) {
        if (!(diagram instanceof IERDiagram)) {
            return 0;
        }

        IERDiagram erDiagram = (IERDiagram) diagram;
        int count = 0;

        try {
            IPresentation[] presentations = erDiagram.getPresentations();
            for (IPresentation presentation : presentations) {
                try {
                    if (presentation.getModel() instanceof IEREntity) {
                        IEREntity entity = (IEREntity) presentation.getModel();
                        count += applyTranslationsToEREntity(entity, translationMap);
                    }
                } catch (Exception e) {
                    // 일부 프레젠테이션 처리 실패 시 계속 진행
                }
            }
        } catch (Exception e) {
            System.err.println("ER 다이어그램 번역 적용 실패: " + e.getMessage());
        }

        return count;
    }

    private void extractFromEREntity(IEREntity entity, List<DiagramNode> japaneseNodes) {
        try {
            // 논리명 추출
            String logicalName = entity.getLogicalName();
            if (logicalName != null && !logicalName.isEmpty() && JapaneseDetector.containsJapanese(logicalName)) {
                japaneseNodes.add(DiagramNode.builder()
                        .id(entity.getId() + "_logical")
                        .name(logicalName)
                        .type("er_entity_logical")
                        .build());
            }

            // 물리명 추출
            String physicalName = entity.getPhysicalName();
            if (physicalName != null && !physicalName.isEmpty() && JapaneseDetector.containsJapanese(physicalName)) {
                japaneseNodes.add(DiagramNode.builder()
                        .id(entity.getId() + "_physical")
                        .name(physicalName)
                        .type("er_entity_physical")
                        .build());
            }

            // 속성(Attribute) 추출
            IERAttribute[] allAttributes = getAllAttributes(entity);
            for (IERAttribute attr : allAttributes) {
                extractFromERAttribute(attr, japaneseNodes);
            }
        } catch (Exception e) {
            System.err.println("ER 엔티티 추출 실패: " + e.getMessage());
        }
    }

    private void extractFromERAttribute(IERAttribute attr, List<DiagramNode> japaneseNodes) {
        try {
            // 논리명 추출
            String logicalName = attr.getLogicalName();
            if (logicalName != null && !logicalName.isEmpty() && JapaneseDetector.containsJapanese(logicalName)) {
                japaneseNodes.add(DiagramNode.builder()
                        .id(attr.getId() + "_logical")
                        .name(logicalName)
                        .type("er_attribute_logical")
                        .build());
            }

            // 물리명 추출
            String physicalName = attr.getPhysicalName();
            if (physicalName != null && !physicalName.isEmpty() && JapaneseDetector.containsJapanese(physicalName)) {
                japaneseNodes.add(DiagramNode.builder()
                        .id(attr.getId() + "_physical")
                        .name(physicalName)
                        .type("er_attribute_physical")
                        .build());
            }
        } catch (Exception e) {
            System.err.println("ER 속성 추출 실패: " + e.getMessage());
        }
    }

    private int applyTranslationsToEREntity(IEREntity entity, Map<String, String> translationMap) {
        int count = 0;

        try {
            // 논리명 번역
            String logicalId = entity.getId() + "_logical";
            if (translationMap.containsKey(logicalId)) {
                String originalLogical = entity.getLogicalName();
                String translatedLogical = translationMap.get(logicalId);
                entity.setLogicalName(originalLogical + "\n" + translatedLogical);
                count++;
            }

            // 물리명 번역
            String physicalId = entity.getId() + "_physical";
            if (translationMap.containsKey(physicalId)) {
                String originalPhysical = entity.getPhysicalName();
                String translatedPhysical = translationMap.get(physicalId);
                entity.setPhysicalName(originalPhysical + "\n" + translatedPhysical);
                count++;
            }

            // 속성 번역
            IERAttribute[] allAttributes = getAllAttributes(entity);
            for (IERAttribute attr : allAttributes) {
                count += applyTranslationsToERAttribute(attr, translationMap);
            }
        } catch (Exception e) {
            System.err.println("ER 엔티티 번역 적용 실패: " + e.getMessage());
        }

        return count;
    }

    private int applyTranslationsToERAttribute(IERAttribute attr, Map<String, String> translationMap) {
        int count = 0;

        try {
            // 논리명 번역
            String logicalId = attr.getId() + "_logical";
            if (translationMap.containsKey(logicalId)) {
                String originalLogical = attr.getLogicalName();
                String translatedLogical = translationMap.get(logicalId);
                attr.setLogicalName(originalLogical + "\n" + translatedLogical);
                count++;
            }

            // 물리명 번역
            String physicalId = attr.getId() + "_physical";
            if (translationMap.containsKey(physicalId)) {
                String originalPhysical = attr.getPhysicalName();
                String translatedPhysical = translationMap.get(physicalId);
                attr.setPhysicalName(originalPhysical + "\n" + translatedPhysical);
                count++;
            }
        } catch (Exception e) {
            System.err.println("ER 속성 번역 적용 실패: " + e.getMessage());
        }

        return count;
    }

    private IERAttribute[] getAllAttributes(IEREntity entity) {
        try {
            IERAttribute[] primaryKeys = entity.getPrimaryKeys();
            IERAttribute[] nonPrimaryKeys = entity.getNonPrimaryKeys();
            IERAttribute[] allAttributes = new IERAttribute[primaryKeys.length + nonPrimaryKeys.length];
            System.arraycopy(primaryKeys, 0, allAttributes, 0, primaryKeys.length);
            System.arraycopy(nonPrimaryKeys, 0, allAttributes, primaryKeys.length, nonPrimaryKeys.length);
            return allAttributes;
        } catch (Exception e) {
            return new IERAttribute[0];
        }
    }
}
