package com.jatoko.service.extractor;

import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.jatoko.model.DiagramNode;
import com.jatoko.util.JapaneseDetector;
import com.jatoko.util.KoreanDetector;

import java.util.List;

/**
 * 다이어그램 추출기의 공통 기능을 제공하는 추상 클래스
 */
public abstract class BaseExtractor {

    /**
     * 프레젠테이션 요소에서 일본어 라벨을 추출합니다.
     */
    protected void extractFromPresentation(IPresentation presentation, List<DiagramNode> japaneseNodes) {
        try {
            String label = presentation.getLabel();
            if (label != null && !label.isEmpty() && JapaneseDetector.containsJapanese(label)) {
                String presentationId = generateStablePresentationId(presentation, label);
                String type = presentation.getType();

                japaneseNodes.add(DiagramNode.builder()
                        .id(presentationId)
                        .name(label)
                        .type("presentation_" + (type != null ? type.toLowerCase() : "unknown"))
                        .build());
            }
        } catch (Exception e) {
            // 일부 프레젠테이션 요소는 getLabel()이 실패할 수 있음
        }
    }

    /**
     * 프레젠테이션 요소의 안정적인 ID를 생성합니다.
     * 모델 ID를 우선 사용하여 라벨 변경에 영향받지 않도록 합니다.
     */
    protected String generateStablePresentationId(IPresentation presentation, String label) {
        try {
            // 모델 ID가 있으면 그것을 직접 사용 (가장 안정적)
            if (presentation.getModel() != null && presentation.getModel().getId() != null) {
                return "presentation_" + presentation.getModel().getId();
            }

            // 모델 ID가 없으면 타입 + 원본 라벨 해시 사용
            // (주의: 라벨은 번역 후 변경되므로 이 경로는 불안정함)
            StringBuilder idBuilder = new StringBuilder();
            String type = presentation.getType();
            if (type != null) {
                idBuilder.append(type).append("_");
            }
            idBuilder.append(Math.abs(label.hashCode()));

            return "presentation_" + idBuilder.toString();
        } catch (Exception e) {
            return "presentation_" + Math.abs(label.hashCode());
        }
    }

    /**
     * 프레젠테이션 요소에 번역을 적용합니다.
     */
    protected int applyTranslationsToPresentations(IPresentation[] presentations,
                                                   java.util.Map<String, String> translationMap) {
        int count = 0;
        int matchFailures = 0;

        for (IPresentation presentation : presentations) {
            try {
                String label = presentation.getLabel();
                if (label != null && !label.isEmpty()) {

                    // 이미 한글이 포함되어 있으면 스킵 (이미 번역된 요소)
                    if (KoreanDetector.containsKorean(label)) {
                        continue;
                    }

                    String presentationId = generateStablePresentationId(presentation, label);

                    if (translationMap.containsKey(presentationId)) {
                        String translatedLabel = translationMap.get(presentationId);

                        // Presentation 타입에 따라 적절한 구분자 사용
                        String separator = getPresentationSeparator(presentation);
                        presentation.setLabel(label + separator + translatedLabel);
                        count++;
                    } else {
                        matchFailures++;
                        if (matchFailures <= 5) {
                            System.err.println("Presentation ID 매칭 실패: " + presentationId +
                                " (타입: " + presentation.getType() + ", 라벨: " +
                                label.substring(0, Math.min(30, label.length())) + "...)");
                        }
                    }
                }
            } catch (Exception e) {
                // 일부 프레젠테이션 요소는 getLabel()/setLabel()이 실패할 수 있음
            }
        }

        if (matchFailures > 5) {
            System.err.println("... 외 " + (matchFailures - 5) + "개 presentation ID 매칭 실패");
        }

        return count;
    }

    /**
     * Presentation 타입에 따라 적절한 구분자를 반환합니다.
     * ActivityFinal 등 일부 타입은 개행문자를 지원하지 않으므로 / 구분자를 사용합니다.
     *
     * @param presentation Presentation 요소
     * @return 구분자 문자열
     */
    private String getPresentationSeparator(IPresentation presentation) {
        try {
            String type = presentation.getType();
            if (type != null) {
                String lowerType = type.toLowerCase();
                // ActivityFinal, ActivityInitial, Partition 등 Activity 관련 Presentation은 / 사용
                if (lowerType.contains("activity") || lowerType.contains("partition")) {
                    return " / ";
                }
            }
        } catch (Exception e) {
            // getType() 실패 시 기본값 사용
        }
        // 기본값: 개행문자
        return "\n";
    }
}
