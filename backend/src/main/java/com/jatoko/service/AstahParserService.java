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

package com.jatoko.service;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.model.IModel;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.jatoko.model.DiagramNode;
import com.jatoko.service.applier.DiagramTranslationApplier;
import com.jatoko.service.applier.ModelTranslationApplier;
import com.jatoko.service.applier.TranslationApplier;
import com.jatoko.service.extractor.NodeExtractor;
import com.jatoko.service.translator.TranslationMapBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Astah 파일 파싱 서비스 (Facade)
 *
 * Astah Professional SDK를 사용하여 .asta 파일을 파싱하고 수정합니다.
 * 실제 작업은 전문 컴포넌트들에게 위임합니다.
 */
@Service
public class AstahParserService extends BaseParserService<DiagramNode> {

    private final NodeExtractor nodeExtractor;
    private final TranslationMapBuilder translationMapBuilder;
    private final TranslationApplier translationApplier;
    private final ModelTranslationApplier modelApplier;
    private final DiagramTranslationApplier diagramApplier;

    public AstahParserService(NodeExtractor nodeExtractor,
                              TranslationMapBuilder translationMapBuilder,
                              TranslationApplier translationApplier,
                              ModelTranslationApplier modelApplier,
                              DiagramTranslationApplier diagramApplier,
                              MetadataService metadataService,
                              com.jatoko.service.translator.Translator translator) {
        super(metadataService, translator);
        this.nodeExtractor = nodeExtractor;
        this.translationMapBuilder = translationMapBuilder;
        this.translationApplier = translationApplier;
        this.modelApplier = modelApplier;
        this.diagramApplier = diagramApplier;
    }

    /**
     * Astah 파일에서 일본어 노드를 추출합니다 (public API).
     */
    public List<DiagramNode> extractJapaneseNodes(File inputFile) {
        return extractNodes(inputFile);
    }

    @Override
    protected List<DiagramNode> extractNodes(File inputFile) {
        ProjectAccessor projectAccessor = null;
        try {
            AstahAPI api = AstahAPI.getAstahAPI();
            projectAccessor = api.getProjectAccessor();
            projectAccessor.open(inputFile.getAbsolutePath());

            return nodeExtractor.extractJapaneseNodes(projectAccessor);

        } catch (Exception e) {
            throw new RuntimeException("Astah 파일 파싱 실패: " + e.getMessage(), e);
        } finally {
            closeProjectAccessor(projectAccessor);
        }
    }

    @Override
    protected void applyTranslationsInternal(File inputFile, List<DiagramNode> nodes, File outputFile) {
        ProjectAccessor projectAccessor = null;
        try {
            AstahAPI api = AstahAPI.getAstahAPI();
            projectAccessor = api.getProjectAccessor();
            projectAccessor.open(inputFile.getAbsolutePath());

            IModel project = projectAccessor.getProject();

            // 번역 노드를 ID → translatedName 맵으로 변환
            Map<String, String> translationMap = translationMapBuilder.buildSimpleTranslationMap(nodes);

            int updatedCount = 0;

            // 모델 요소 번역 적용
            updatedCount += modelApplier.applyTranslations(project, translationMap, null, 0, 0);

            // 다이어그램 번역 적용
            updatedCount += diagramApplier.applyTranslationsIntegrated(projectAccessor, nodes, translationMap);

            // 파일 저장
            projectAccessor.saveAs(outputFile.getAbsolutePath());

            System.out.println("=== 통합 번역 적용 완료 ===");
            System.out.println("총 " + updatedCount + "개 요소 수정");

        } catch (Exception e) {
            throw new RuntimeException("통합 번역 적용 실패: " + e.getMessage(), e);
        } finally {
            closeProjectAccessor(projectAccessor);
        }
    }

    @Override
    protected String getId(DiagramNode node) {
        return node.getId();
    }

    @Override
    protected String getOriginalText(DiagramNode node) {
        return node.getName();
    }

    @Override
    protected void setTranslatedText(DiagramNode node, String text) {
        node.setTranslatedName(text);
    }

    @Override
    protected String getTranslatedText(DiagramNode node) {
        return node.getTranslatedName();
    }

    @Override
    protected boolean isDuplicate(DiagramNode node) {
        return node.isDuplicate();
    }

    @Override
    protected void postProcessTranslations(List<DiagramNode> allNodes, List<DiagramNode> translatedNodes) {
        // 중복 노드들은 대표 노드의 번역을 복사
        for (DiagramNode node : allNodes) {
            if (node.isDuplicate()) {
                // 같은 이름을 가진 대표 노드를 찾아서 번역을 복사
                for (DiagramNode representative : allNodes) {
                    if (!representative.isDuplicate() && representative.getName().equals(node.getName())) {
                        node.setTranslatedName(representative.getTranslatedName());
                        break;
                    }
                }
            }
        }
    }

    private void closeProjectAccessor(ProjectAccessor projectAccessor) {
        if (projectAccessor != null) {
            try {
                projectAccessor.close();
            } catch (Exception e) {
                System.err.println("ProjectAccessor 닫기 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 번역된 이름을 Astah 파일에 적용합니다 (진행 상황 콜백 포함).
     * (기존 메서드 유지 - 별도 사용 가능성 고려)
     */
    public void applyTranslations(File astahFile, List<DiagramNode> nodes, File outputFile,
                                  Consumer<Integer> progressCallback) {
        ProjectAccessor projectAccessor = null;
        try {
            AstahAPI api = AstahAPI.getAstahAPI();
            projectAccessor = api.getProjectAccessor();
            projectAccessor.open(astahFile.getAbsolutePath());

            // 타입별 번역 맵 생성
            Map<String, Map<String, String>> translationMaps = translationMapBuilder.buildTranslationMaps(nodes, progressCallback);

            if (progressCallback != null) {
                progressCallback.accept(30);
            }

            // 번역 적용
            int updatedCount = translationApplier.applyTranslations(projectAccessor, translationMaps, progressCallback);

            if (progressCallback != null) {
                progressCallback.accept(90);
            }

            projectAccessor.saveAs(outputFile.getAbsolutePath());

            if (progressCallback != null) {
                progressCallback.accept(100);
            }

            System.out.println("번역 적용 완료: " + updatedCount + "개 요소 수정");
        } catch (Exception e) {
            throw new RuntimeException("Astah 파일 수정 실패: " + e.getMessage(), e);
        } finally {
            closeProjectAccessor(projectAccessor);
        }
    }
}
