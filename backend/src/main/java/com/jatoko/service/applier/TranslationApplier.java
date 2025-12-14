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

import com.change_vision.jude.api.inf.model.IModel;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 번역 적용을 위한 Facade 클래스
 * ModelTranslationApplier와 DiagramTranslationApplier를 조율합니다.
 */
@Component
public class TranslationApplier {

    private final ModelTranslationApplier modelApplier;
    private final DiagramTranslationApplier diagramApplier;

    public TranslationApplier(ModelTranslationApplier modelApplier,
                              DiagramTranslationApplier diagramApplier) {
        this.modelApplier = modelApplier;
        this.diagramApplier = diagramApplier;
    }

    /**
     * 모델과 다이어그램에 번역을 적용합니다.
     *
     * @param projectAccessor Astah ProjectAccessor
     * @param translationMaps 타입별 번역 맵
     * @param progressCallback 진행 상황 콜백 (optional)
     * @return 총 수정된 요소 수
     */
    public int applyTranslations(ProjectAccessor projectAccessor,
                                  Map<String, Map<String, String>> translationMaps,
                                  Consumer<Integer> progressCallback) {
        int totalCount = 0;

        try {
            IModel project = projectAccessor.getProject();

            // 모델 요소 번역 적용 (30% ~ 60%)
            Map<String, String> modelMap = translationMaps.get("model");
            if (modelMap != null && !modelMap.isEmpty()) {
                totalCount += modelApplier.applyTranslations(
                    project, modelMap, progressCallback, 30, 60
                );
            }

            // 다이어그램 번역 적용 (60% ~ 90%)
            totalCount += diagramApplier.applyTranslations(
                projectAccessor, translationMaps, progressCallback, 60, 90
            );

            // 주석/노트 번역
            Map<String, String> commentMap = translationMaps.get("comment");
            if (commentMap != null && !commentMap.isEmpty()) {
                totalCount += modelApplier.applyTranslationsToComments(project, commentMap);
            }

        } catch (Exception e) {
            System.err.println("번역 적용 실패: " + e.getMessage());
            throw new RuntimeException("번역 적용 실패: " + e.getMessage(), e);
        }

        return totalCount;
    }
}
