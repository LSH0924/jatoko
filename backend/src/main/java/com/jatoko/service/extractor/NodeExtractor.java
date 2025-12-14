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
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.jatoko.model.DiagramNode;
import com.jatoko.util.JapaneseDetector;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Astah 파일에서 일본어 노드를 추출하는 컴포넌트
 */
@Component
public class NodeExtractor {

    private final List<DiagramExtractor> extractors;

    public NodeExtractor(List<DiagramExtractor> extractors) {
        this.extractors = extractors;
    }

    /**
     * 프로젝트에서 일본어 노드를 추출합니다.
     *
     * @param projectAccessor Astah ProjectAccessor
     * @return 일본어 노드 리스트
     */
    public List<DiagramNode> extractJapaneseNodes(ProjectAccessor projectAccessor) {
        List<DiagramNode> japaneseNodes = new ArrayList<>();

        try {
            IModel project = projectAccessor.getProject();

            // 모든 모델 요소 탐색
            traverseModel(project, japaneseNodes);

            // 다이어그램 요소 탐색
            extractJapaneseNodesFromDiagrams(projectAccessor, japaneseNodes);

            // 중복 단어 감지 및 처리
            processDuplicates(japaneseNodes);

        } catch (Exception e) {
            throw new RuntimeException("일본어 노드 추출 실패: " + e.getMessage(), e);
        }

        return japaneseNodes;
    }

    /**
     * 다이어그램에서 일본어 노드를 추출합니다.
     */
    private void extractJapaneseNodesFromDiagrams(ProjectAccessor projectAccessor, List<DiagramNode> japaneseNodes) {
        try {
            IModel project = projectAccessor.getProject();
            // 루트 레벨 다이어그램 추출
            extractDiagramsFromElement(project, japaneseNodes);
            // 패키지 내부 다이어그램 재귀 탐색
            traversePackagesForDiagrams(project, japaneseNodes);
        } catch (Exception e) {
            System.err.println("다이어그램 탐색 실패: " + e.getMessage());
        }
    }

    /**
     * 패키지를 재귀적으로 탐색하여 다이어그램을 추출합니다.
     */
    private void traversePackagesForDiagrams(INamedElement element, List<DiagramNode> japaneseNodes) {
        if (element == null) return;

        if (element instanceof IPackage) {
            IPackage pkg = (IPackage) element;
            try {
                // 현재 패키지의 다이어그램 추출
                extractDiagramsFromElement(pkg, japaneseNodes);

                // 하위 패키지 재귀 탐색
                for (INamedElement child : pkg.getOwnedElements()) {
                    traversePackagesForDiagrams(child, japaneseNodes);
                }
            } catch (Exception e) {
                System.err.println("패키지 다이어그램 탐색 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 주어진 요소(프로젝트 또는 패키지)의 다이어그램을 추출합니다.
     */
    private void extractDiagramsFromElement(INamedElement element, List<DiagramNode> japaneseNodes) {
        try {
            IDiagram[] diagrams = null;
            if (element instanceof IModel) {
                diagrams = ((IModel) element).getDiagrams();
            } else if (element instanceof IPackage) {
                diagrams = ((IPackage) element).getDiagrams();
            }

            if (diagrams != null) {
                for (IDiagram diagram : diagrams) {
                    // 다이어그램 이름 추출
                    extractDiagramName(diagram, japaneseNodes);

                    // 적합한 추출기를 찾아서 처리
                    for (DiagramExtractor extractor : extractors) {
                        if (extractor.supports(diagram)) {
                            extractor.extract(diagram, japaneseNodes);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("다이어그램 추출 실패: " + e.getMessage());
        }
    }

    /**
     * 다이어그램 이름을 추출합니다.
     */
    private void extractDiagramName(IDiagram diagram, List<DiagramNode> japaneseNodes) {
        try {
            String diagramName = diagram.getName();
            if (diagramName != null && !diagramName.isEmpty() && JapaneseDetector.containsJapanese(diagramName)) {
                japaneseNodes.add(DiagramNode.builder()
                        .id("diagram_" + diagram.getId())
                        .name(diagramName)
                        .type("diagram_name")
                        .diagramId(diagram.getId())
                        .build());
            }
        } catch (Exception e) {
            System.err.println("다이어그램 이름 추출 실패: " + e.getMessage());
        }
    }

    /**
     * 모델 요소를 재귀적으로 탐색하여 일본어 이름을 가진 요소를 찾습니다.
     */
    private void traverseModel(INamedElement element, List<DiagramNode> japaneseNodes) {
        if (element == null) return;

        // 노트/주석 처리
        if (element instanceof IComment) {
            extractFromComment((IComment) element, japaneseNodes);
            return;
        }

        String name = element.getName();
        String elementType = getElementType(element);

        // unknown 타입의 요소는 제외 (setName이 불가능한 특수 요소)
        if (!"unknown".equals(elementType) && name != null && !name.isEmpty() && JapaneseDetector.containsJapanese(name)) {
            japaneseNodes.add(DiagramNode.builder()
                    .id(element.getId())
                    .name(name)
                    .type(elementType)
                    .build());
        }

        // 자식 요소 탐색
        if (element instanceof IPackage) {
            IPackage pkg = (IPackage) element;
            try {
                for (INamedElement child : pkg.getOwnedElements()) {
                    traverseModel(child, japaneseNodes);
                }
            } catch (Exception e) {
                // 일부 요소 접근 실패 시 계속 진행
            }
        }

        // 클래스/인터페이스의 속성과 메서드 탐색
        if (element instanceof IClass) {
            IClass cls = (IClass) element;
            try {
                for (IAttribute attr : cls.getAttributes()) {
                    traverseModel(attr, japaneseNodes);
                }
                for (IOperation op : cls.getOperations()) {
                    traverseModel(op, japaneseNodes);
                }
            } catch (Exception e) {
                // 일부 요소 접근 실패 시 계속 진행
            }
        }
    }

    /**
     * 노트/주석에서 일본어 요소를 추출합니다.
     */
    private void extractFromComment(IComment comment, List<DiagramNode> japaneseNodes) {
        try {
            String body = comment.getBody();
            if (body != null && !body.isEmpty() && JapaneseDetector.containsJapanese(body)) {
                japaneseNodes.add(DiagramNode.builder()
                        .id(comment.getId() + "_body")
                        .name(body)
                        .type("comment_body")
                        .build());
            }
        } catch (Exception e) {
            System.err.println("주석 추출 실패: " + e.getMessage());
        }
    }

    /**
     * 요소 타입을 문자열로 반환합니다.
     */
    private String getElementType(INamedElement element) {
        if (element instanceof IUseCase) {
            return "usecase";
        } else if (element instanceof IAction) {
            return "activity_action";
        } else if (element instanceof IActivityNode) {
            return "activity_node";
        } else if (element instanceof IFlow) {
            return "activity_flow";
        } else if (element instanceof IActivity) {
            return "activity";
        } else if (element instanceof IEREntity) {
            return "er_entity";
        } else if (element instanceof IERAttribute) {
            return "er_attribute";
        } else if (element instanceof IMessage) {
            return "sequence_message";
        } else if (element instanceof ILifeline) {
            return "sequence_lifeline";
        } else if (element instanceof IInteraction) {
            return "sequence_interaction";
        } else if (element instanceof IComment) {
            return "comment";
        } else if (element instanceof IClass) {
            return "class";
        } else if (element instanceof IPackage) {
            return "package";
        } else if (element instanceof IAttribute) {
            return "attribute";
        } else if (element instanceof IOperation) {
            return "method";
        }
        return "unknown";
    }

    /**
     * 중복 단어를 감지하고 처리합니다.
     * 동일한 name을 가진 노드들을 그룹화하여 첫 번째 노드만 대표로 유지하고,
     * 나머지는 중복으로 표시합니다.
     *
     * @param japaneseNodes 일본어 노드 리스트
     */
    private void processDuplicates(List<DiagramNode> japaneseNodes) {
        // name별로 그룹화
        Map<String, List<DiagramNode>> nameToNodesMap = new HashMap<>();
        for (DiagramNode node : japaneseNodes) {
            String name = node.getName();
            if (name != null && !name.isEmpty()) {
                nameToNodesMap.computeIfAbsent(name, k -> new ArrayList<>()).add(node);
            }
        }

        // 중복 처리
        int duplicateCount = 0;
        for (Map.Entry<String, List<DiagramNode>> entry : nameToNodesMap.entrySet()) {
            List<DiagramNode> nodes = entry.getValue();
            if (nodes.size() > 1) {
                // 첫 번째 노드를 대표로 설정
                DiagramNode representative = nodes.get(0);
                List<String> duplicateIds = new ArrayList<>();

                // 나머지 노드들을 중복으로 표시
                for (int i = 1; i < nodes.size(); i++) {
                    DiagramNode duplicate = nodes.get(i);
                    duplicate.setDuplicate(true);
                    duplicateIds.add(duplicate.getId());
                }

                // 대표 노드에 중복 ID 목록 설정
                representative.setDuplicateNodeIds(duplicateIds);
                duplicateCount += duplicateIds.size();
            }
        }

        if (duplicateCount > 0) {
            System.out.println("중복 단어 감지: " + duplicateCount + "개 중복 노드 발견");
        }
    }
}
