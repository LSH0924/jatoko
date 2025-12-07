package com.jatoko.service.applier;

import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IModel;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.IPackage;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.jatoko.model.DiagramNode;
import com.jatoko.service.extractor.DiagramExtractor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 다이어그램 요소에 번역을 적용하는 컴포넌트
 */
@Component
public class DiagramTranslationApplier {

    private final List<DiagramExtractor> extractors;

    public DiagramTranslationApplier(List<DiagramExtractor> extractors) {
        this.extractors = extractors;
    }

    /**
     * 다이어그램 요소에 번역을 적용합니다.
     *
     * @param projectAccessor Astah ProjectAccessor
     * @param translationMaps 타입별 번역 맵
     * @param progressCallback 진행 상황 콜백 (optional)
     * @param startProgress 시작 진행률
     * @param endProgress 종료 진행률
     * @return 수정된 요소 수
     */
    public int applyTranslations(ProjectAccessor projectAccessor,
                                  Map<String, Map<String, String>> translationMaps,
                                  Consumer<Integer> progressCallback,
                                  int startProgress, int endProgress) {
        int[] countHolder = new int[]{0};
        int[] processedHolder = new int[]{0};

        try {
            IModel project = projectAccessor.getProject();
            Map<String, String> diagramNameMap = translationMaps.get("diagram");

            // 전체 다이어그램 수 계산 (진행률 표시용)
            int totalDiagrams = countAllDiagrams(project);

            // 루트 레벨 다이어그램 처리
            applyTranslationsToDiagrams(project, translationMaps, diagramNameMap, countHolder,
                                        processedHolder, totalDiagrams, progressCallback, startProgress, endProgress);

            // 패키지 내부 다이어그램 재귀 처리
            traversePackagesForDiagramTranslations(project, translationMaps, diagramNameMap, countHolder,
                                                    processedHolder, totalDiagrams, progressCallback, startProgress, endProgress);

        } catch (Exception e) {
            System.err.println("다이어그램 번역 적용 실패: " + e.getMessage());
        }

        return countHolder[0];
    }

    /**
     * 전체 다이어그램 수를 계산합니다 (패키지 내부 포함).
     */
    private int countAllDiagrams(INamedElement element) {
        int count = 0;

        try {
            if (element instanceof IModel) {
                count += ((IModel) element).getDiagrams().length;
            } else if (element instanceof IPackage) {
                count += ((IPackage) element).getDiagrams().length;
            }

            if (element instanceof IPackage) {
                for (INamedElement child : ((IPackage) element).getOwnedElements()) {
                    count += countAllDiagrams(child);
                }
            }
        } catch (Exception e) {
            // 무시
        }

        return count;
    }

    /**
     * 패키지를 재귀적으로 탐색하여 다이어그램에 번역을 적용합니다.
     */
    private void traversePackagesForDiagramTranslations(INamedElement element,
                                                         Map<String, Map<String, String>> translationMaps,
                                                         Map<String, String> diagramNameMap,
                                                         int[] countHolder, int[] processedHolder,
                                                         int totalDiagrams, Consumer<Integer> progressCallback,
                                                         int startProgress, int endProgress) {
        if (element == null) return;

        if (element instanceof IPackage) {
            IPackage pkg = (IPackage) element;
            try {
                // 현재 패키지의 다이어그램 처리
                applyTranslationsToDiagrams(pkg, translationMaps, diagramNameMap, countHolder,
                                            processedHolder, totalDiagrams, progressCallback, startProgress, endProgress);

                // 하위 패키지 재귀 탐색
                for (INamedElement child : pkg.getOwnedElements()) {
                    traversePackagesForDiagramTranslations(child, translationMaps, diagramNameMap, countHolder,
                                                            processedHolder, totalDiagrams, progressCallback, startProgress, endProgress);
                }
            } catch (Exception e) {
                System.err.println("패키지 다이어그램 번역 탐색 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 주어진 요소(프로젝트 또는 패키지)의 다이어그램에 번역을 적용합니다.
     */
    private void applyTranslationsToDiagrams(INamedElement element,
                                              Map<String, Map<String, String>> translationMaps,
                                              Map<String, String> diagramNameMap,
                                              int[] countHolder, int[] processedHolder,
                                              int totalDiagrams, Consumer<Integer> progressCallback,
                                              int startProgress, int endProgress) {
        try {
            IDiagram[] diagrams = null;
            if (element instanceof IModel) {
                diagrams = ((IModel) element).getDiagrams();
            } else if (element instanceof IPackage) {
                diagrams = ((IPackage) element).getDiagrams();
            }

            if (diagrams != null) {
                for (IDiagram diagram : diagrams) {
                    // 다이어그램 이름 번역
                    if (diagramNameMap != null && diagramNameMap.containsKey(diagram.getId())) {
                        try {
                            String originalName = diagram.getName();
                            String translatedName = diagramNameMap.get(diagram.getId());
                            diagram.setName(originalName + "\n" + translatedName);
                            countHolder[0]++;
                        } catch (Exception e) {
                            System.err.println("다이어그램 이름 변경 실패: " + e.getMessage());
                        }
                    }

                    // 적합한 추출기를 찾아서 번역 적용
                    for (DiagramExtractor extractor : extractors) {
                        if (extractor.supports(diagram)) {
                            String extractorName = extractor.getClass().getSimpleName();
                            Map<String, String> translationMap = translationMaps.get(extractorName);
                            if (translationMap != null && !translationMap.isEmpty()) {
                                int beforeCount = countHolder[0];
                                countHolder[0] += extractor.applyTranslations(diagram, translationMap);
                                int applied = countHolder[0] - beforeCount;
                                System.out.println(extractorName + "가 " + diagram.getName() + "에 " + applied + "개 적용 (맵 크기: " + translationMap.size() + ")");
                            }
                            break;
                        }
                    }

                    processedHolder[0]++;
                    if (progressCallback != null && totalDiagrams > 0) {
                        int progress = startProgress + (int) ((processedHolder[0] * (endProgress - startProgress)) / (double) totalDiagrams);
                        progressCallback.accept(progress);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("다이어그램 번역 적용 실패: " + e.getMessage());
        }
    }

    /**
     * 통합 처리용: 다이어그램 번역 적용
     *
     * @param projectAccessor Astah ProjectAccessor
     * @param translationNodes 번역된 노드 리스트
     * @param translationMap 통합 번역 맵
     * @return 수정된 요소 수
     */
    public int applyTranslationsIntegrated(ProjectAccessor projectAccessor,
                                            List<DiagramNode> translationNodes,
                                            Map<String, String> translationMap) {
        int[] countHolder = new int[]{0};

        try {
            IModel project = projectAccessor.getProject();

            // 다이어그램 이름 번역용 맵
            Map<String, String> diagramNameMap = new HashMap<>();
            for (DiagramNode node : translationNodes) {
                if ("diagram_name".equals(node.getType()) && node.getDiagramId() != null) {
                    diagramNameMap.put(node.getDiagramId(), node.getTranslatedName());
                }
            }

            // 루트 레벨 다이어그램 처리
            applyTranslationsIntegratedToDiagrams(project, diagramNameMap, translationMap, countHolder);

            // 패키지 내부 다이어그램 재귀 처리
            traversePackagesForIntegratedDiagramTranslations(project, diagramNameMap, translationMap, countHolder);

        } catch (Exception e) {
            System.err.println("다이어그램 번역 적용 실패: " + e.getMessage());
        }

        return countHolder[0];
    }

    /**
     * 패키지를 재귀적으로 탐색하여 통합 다이어그램 번역을 적용합니다.
     */
    private void traversePackagesForIntegratedDiagramTranslations(INamedElement element,
                                                                   Map<String, String> diagramNameMap,
                                                                   Map<String, String> translationMap,
                                                                   int[] countHolder) {
        if (element == null) return;

        if (element instanceof IPackage) {
            IPackage pkg = (IPackage) element;
            try {
                // 현재 패키지의 다이어그램 처리
                applyTranslationsIntegratedToDiagrams(pkg, diagramNameMap, translationMap, countHolder);

                // 하위 패키지 재귀 탐색
                for (INamedElement child : pkg.getOwnedElements()) {
                    traversePackagesForIntegratedDiagramTranslations(child, diagramNameMap, translationMap, countHolder);
                }
            } catch (Exception e) {
                System.err.println("패키지 통합 다이어그램 번역 탐색 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 주어진 요소(프로젝트 또는 패키지)의 다이어그램에 통합 번역을 적용합니다.
     */
    private void applyTranslationsIntegratedToDiagrams(INamedElement element,
                                                        Map<String, String> diagramNameMap,
                                                        Map<String, String> translationMap,
                                                        int[] countHolder) {
        try {
            IDiagram[] diagrams = null;
            if (element instanceof IModel) {
                diagrams = ((IModel) element).getDiagrams();
            } else if (element instanceof IPackage) {
                diagrams = ((IPackage) element).getDiagrams();
            }

            if (diagrams != null) {
                for (IDiagram diagram : diagrams) {
                    // 다이어그램 이름 번역
                    if (diagramNameMap.containsKey(diagram.getId())) {
                        try {
                            String originalName = diagram.getName();
                            String translatedName = diagramNameMap.get(diagram.getId());
                            diagram.setName(originalName + " / " + translatedName);
                            countHolder[0]++;
                            System.out.println("다이어그램 이름 번역: " + diagram.getId() + " → " + translatedName);
                        } catch (Exception e) {
                            System.err.println("다이어그램 이름 변경 실패: " + e.getMessage());
                        }
                    }

                    // 다이어그램 내부 요소 번역 (Frame, Partition, Note 등)
                    for (DiagramExtractor extractor : extractors) {
                        if (extractor.supports(diagram)) {
                            int beforeCount = countHolder[0];
                            countHolder[0] += extractor.applyTranslations(diagram, translationMap);
                            int applied = countHolder[0] - beforeCount;
                            if (applied > 0) {
                                System.out.println(extractor.getClass().getSimpleName() + "가 " + diagram.getName() + "에 " + applied + "개 적용");
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("통합 다이어그램 번역 적용 실패: " + e.getMessage());
        }
    }
}
