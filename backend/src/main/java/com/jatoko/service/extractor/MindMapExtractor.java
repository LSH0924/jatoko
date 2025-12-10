package com.jatoko.service.extractor;

import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IMindMapDiagram;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.jatoko.model.DiagramNode;
import com.jatoko.util.JapaneseDetector;
import com.jatoko.util.KoreanDetector;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 마인드맵 추출기
 */
@Component
@Order(3)
public class MindMapExtractor extends BaseExtractor implements DiagramExtractor {

    @Override
    public boolean supports(IDiagram diagram) {
        return diagram instanceof IMindMapDiagram;
    }

    @Override
    public void extract(IDiagram diagram, List<DiagramNode> japaneseNodes) {
        if (!(diagram instanceof IMindMapDiagram)) {
            return;
        }

        IMindMapDiagram mindMap = (IMindMapDiagram) diagram;

        try {
            INodePresentation rootTopic = mindMap.getRoot();
            if (rootTopic != null) {
                extractFromTopic(rootTopic, japaneseNodes);
            }

            // Floating topics도 확인
            INodePresentation[] floatingTopics = mindMap.getFloatingTopics();
            for (INodePresentation topic : floatingTopics) {
                extractFromTopic(topic, japaneseNodes);
            }
        } catch (Exception e) {
            System.err.println("마인드맵 추출 실패: " + e.getMessage());
        }
    }

    private int matchFailureCount = 0;
    private static final int MAX_FAILURE_LOG = 5;

    @Override
    public int applyTranslations(IDiagram diagram, Map<String, String> translationMap) {
        if (!(diagram instanceof IMindMapDiagram)) {
            return 0;
        }

        IMindMapDiagram mindMap = (IMindMapDiagram) diagram;
        int count = 0;
        matchFailureCount = 0;

        try {
            INodePresentation rootTopic = mindMap.getRoot();
            if (rootTopic != null) {
                count += applyTranslationsToTopic(rootTopic, translationMap);
            }

            // Floating topics에도 적용
            INodePresentation[] floatingTopics = mindMap.getFloatingTopics();
            for (INodePresentation topic : floatingTopics) {
                count += applyTranslationsToTopic(topic, translationMap);
            }

            if (matchFailureCount > MAX_FAILURE_LOG) {
                System.err.println("... 외 " + (matchFailureCount - MAX_FAILURE_LOG) + "개 토픽 ID 매칭 실패");
            }
        } catch (Exception e) {
            System.err.println("마인드맵 번역 적용 실패: " + e.getMessage());
        }

        return count;
    }

    /**
     * 토픽을 재귀적으로 탐색하여 일본어를 포함한 토픽을 추출합니다.
     */
    private void extractFromTopic(INodePresentation topic, List<DiagramNode> japaneseNodes) {
        if (topic == null) return;

        try {
            String label = topic.getLabel();
            if (label != null && !label.isEmpty() && JapaneseDetector.containsJapanese(label)) {
                // 안정적인 ID 생성
                String topicId = generateStableTopicId(topic, label);
                japaneseNodes.add(DiagramNode.builder()
                        .id(topicId)
                        .name(label)
                        .type("mindmap_topic")
                        .build());
            }

            // 자식 토픽 탐색
            INodePresentation[] children = topic.getChildren();
            for (INodePresentation child : children) {
                extractFromTopic(child, japaneseNodes);
            }
        } catch (Exception e) {
            System.err.println("토픽 추출 실패: " + e.getMessage());
        }
    }

    /**
     * 토픽을 재귀적으로 탐색하여 번역을 적용합니다.
     */
    private int applyTranslationsToTopic(INodePresentation topic, Map<String, String> translationMap) {
        if (topic == null) return 0;

        int count = 0;

        try {
            String label = topic.getLabel();
            if (label != null && !label.isEmpty()) {
                // 이미 한글이 포함되어 있으면 스킵 (이미 번역된 요소)
                if (KoreanDetector.containsKorean(label)) {
                    // 자식 토픽은 계속 처리
                } else {
                    // 추출 시와 동일한 방식으로 ID 생성
                    String topicId = generateStableTopicId(topic, label);

                    if (translationMap.containsKey(topicId)) {
                        String translatedLabel = translationMap.get(topicId);
                        // 원문 아래 줄바꿈 후 번역본 추가
                        topic.setLabel(label + "\n" + translatedLabel);
                        count++;
                    } else {
                        matchFailureCount++;
                        if (matchFailureCount <= MAX_FAILURE_LOG) {
                            System.err.println("마인드맵 토픽 ID 매칭 실패: " + topicId + " (라벨: " + label + ")");
                        }
                    }
                }
            }

            // 자식 토픽 처리
            INodePresentation[] children = topic.getChildren();
            for (INodePresentation child : children) {
                count += applyTranslationsToTopic(child, translationMap);
            }
        } catch (Exception e) {
            System.err.println("토픽 번역 적용 실패: " + e.getMessage());
        }

        return count;
    }

    /**
     * 토픽의 안정적인 ID를 생성합니다.
     * 모델 ID를 직접 사용하여 파일을 다시 열어도 동일한 ID가 생성되도록 합니다.
     */
    private String generateStableTopicId(INodePresentation topic, String label) {
        try {
            // 모델 ID가 있으면 그것을 직접 사용 (가장 안정적)
            if (topic.getModel() != null && topic.getModel().getId() != null) {
                return "topic_" + topic.getModel().getId();
            }

            // 모델 ID가 없으면 프레젠테이션 ID 사용
            // (주의: 라벨은 번역 후 변경되므로 사용 불가)
            return "topic_" + Math.abs(label.hashCode());
        } catch (Exception e) {
            // 실패 시 라벨 해시로 폴백
            return "topic_" + Math.abs(label.hashCode());
        }
    }
}
