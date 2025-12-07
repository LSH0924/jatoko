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
 * 액티비티 다이어그램 추출기
 */
@Component
@Order(4)
public class ActivityDiagramExtractor extends BaseExtractor implements DiagramExtractor {

    @Override
    public boolean supports(IDiagram diagram) {
        return diagram instanceof IActivityDiagram;
    }

    @Override
    public void extract(IDiagram diagram, List<DiagramNode> japaneseNodes) {
        if (!(diagram instanceof IActivityDiagram)) {
            return;
        }

        IActivityDiagram activityDiagram = (IActivityDiagram) diagram;

        try {
            IActivity activity = activityDiagram.getActivity();
            if (activity != null) {
                // 액티비티 자체의 이름 확인
                extractFromNamedElement(activity, japaneseNodes);

                // 액티비티 노드들 탐색
                IActivityNode[] nodes = activity.getActivityNodes();
                for (IActivityNode node : nodes) {
                    extractFromNamedElement(node, japaneseNodes);
                }

                // 플로우(전이) 탐색
                IFlow[] flows = activity.getFlows();
                for (IFlow flow : flows) {
                    extractFromNamedElement(flow, japaneseNodes);
                }
            }

            // 프레젠테이션 요소 추출 (노드 라벨 등)
            IPresentation[] presentations = activityDiagram.getPresentations();
            for (IPresentation presentation : presentations) {
                extractFromPresentation(presentation, japaneseNodes);
            }
        } catch (Exception e) {
            System.err.println("액티비티 다이어그램 추출 실패: " + e.getMessage());
        }
    }

    @Override
    public int applyTranslations(IDiagram diagram, Map<String, String> translationMap) {
        if (!(diagram instanceof IActivityDiagram)) {
            return 0;
        }

        IActivityDiagram activityDiagram = (IActivityDiagram) diagram;
        int count = 0;

        try {
            IActivity activity = activityDiagram.getActivity();
            if (activity != null) {
                // 액티비티 이름 번역
                count += applyTranslationToNamedElement(activity, translationMap);

                // 액티비티 노드들 번역
                IActivityNode[] nodes = activity.getActivityNodes();
                for (IActivityNode node : nodes) {
                    count += applyTranslationToNamedElement(node, translationMap);
                }

                // 플로우(전이) 번역
                IFlow[] flows = activity.getFlows();
                for (IFlow flow : flows) {
                    count += applyTranslationToNamedElement(flow, translationMap);
                }
            }

            // 프레젠테이션 요소 번역
            IPresentation[] presentations = activityDiagram.getPresentations();
            count += applyTranslationsToPresentations(presentations, translationMap);
        } catch (Exception e) {
            System.err.println("액티비티 다이어그램 번역 적용 실패: " + e.getMessage());
        }

        return count;
    }

    /**
     * 네임드 엘리먼트에서 일본어를 추출합니다.
     */
    private void extractFromNamedElement(INamedElement element, List<DiagramNode> japaneseNodes) {
        if (element == null) return;

        try {
            String name = element.getName();
            if (name != null && !name.isEmpty() && JapaneseDetector.containsJapanese(name)) {
                // 제어 노드(이름 변경 불가능한 노드)는 제외
                if (element instanceof IActivityNode && isImmutableControlNode(name)) {
                    return;
                }

                japaneseNodes.add(DiagramNode.builder()
                        .id(element.getId())
                        .name(name)
                        .type(getElementType(element))
                        .build());
            }
        } catch (Exception e) {
            System.err.println("네임드 엘리먼트 추출 실패: " + e.getMessage());
        }
    }

    /**
     * 변경 불가능한 제어 노드인지 확인합니다.
     * 시작 노드, 종료 노드, 결정/병합 노드, 포크/조인 노드는 이름 변경이 불가능합니다.
     */
    private boolean isImmutableControlNode(String nodeName) {
        if (nodeName == null) return false;

        // Astah의 제어 노드 기본 이름 패턴
        return nodeName.matches("開始ノード\\d*") ||           // 시작 노드
               nodeName.matches("終了ノード\\d*") ||           // 종료 노드
               nodeName.matches("フロー終了ノード\\d*") ||     // 플로우 종료 노드
               nodeName.matches("デシジョンノード.*") ||       // 결정 노드
               nodeName.matches("マージノード.*") ||           // 병합 노드
               nodeName.matches("フォークノード.*") ||         // 포크 노드
               nodeName.matches("ジョインノード.*");           // 조인 노드
    }

    /**
     * 네임드 엘리먼트에 번역을 적용합니다.
     */
    private int applyTranslationToNamedElement(INamedElement element, Map<String, String> translationMap) {
        if (element == null) return 0;

        int count = 0;

        try {
            String elementId = element.getId();
            if (translationMap.containsKey(elementId)) {
                String originalName = element.getName();
                String translatedName = translationMap.get(elementId);
                // 원문 아래 줄바꿈 후 번역본 추가
                element.setName(originalName + "\n" + translatedName);
                count++;
            }
        } catch (Exception e) {
            System.err.println("요소 이름 변경 실패: " + element.getId() + " - " + e.getMessage());
        }

        return count;
    }

    /**
     * 요소 타입을 문자열로 반환합니다.
     */
    private String getElementType(INamedElement element) {
        if (element instanceof IAction) {
            return "activity_action";
        } else if (element instanceof IActivityNode) {
            return "activity_node";
        } else if (element instanceof IFlow) {
            return "activity_flow";
        } else if (element instanceof IActivity) {
            return "activity";
        }
        return "activity_unknown";
    }
}
