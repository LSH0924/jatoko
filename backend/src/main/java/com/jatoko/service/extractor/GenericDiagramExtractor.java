package com.jatoko.service.extractor;

import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.jatoko.model.DiagramNode;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 일반 다이어그램 추출기
 * Use Case, Class 등 대부분의 다이어그램에서 사용됩니다.
 * 특별한 처리가 필요하지 않은 모든 다이어그램의 프레젠테이션 요소를 추출합니다.
 */
@Component
@Order(100) // 가장 낮은 우선순위 - 다른 추출기들이 먼저 처리되도록
public class GenericDiagramExtractor extends BaseExtractor implements DiagramExtractor {

    @Override
    public boolean supports(IDiagram diagram) {
        // 모든 다이어그램을 지원하지만, 우선순위가 가장 낮음 (다른 추출기가 없을 때 사용)
        return true;
    }

    @Override
    public void extract(IDiagram diagram, List<DiagramNode> japaneseNodes) {
        try {
            IPresentation[] presentations = diagram.getPresentations();
            for (IPresentation presentation : presentations) {
                extractFromPresentation(presentation, japaneseNodes);
            }
        } catch (Exception e) {
            System.err.println("일반 다이어그램 추출 실패: " + e.getMessage());
        }
    }

    @Override
    public int applyTranslations(IDiagram diagram, Map<String, String> translationMap) {
        int count = 0;

        try {
            IPresentation[] presentations = diagram.getPresentations();
            count += applyTranslationsToPresentations(presentations, translationMap);
        } catch (Exception e) {
            System.err.println("일반 다이어그램 번역 적용 실패: " + e.getMessage());
        }

        return count;
    }
}
