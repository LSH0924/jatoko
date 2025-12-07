package com.jatoko.service;

import com.jatoko.model.DiagramNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AstahParserServiceTest {

    @Autowired
    private AstahParserService astahParserService;

    @Test
    void testExtractJapaneseNodes() {
        // test.asta 파일 경로
        File testFile = new File("../../test.asta");
        if (!testFile.exists()) {
            testFile = new File("../test.asta");
        }
        if (!testFile.exists()) {
            testFile = new File("test.asta");
        }
        assertTrue(testFile.exists(), "test.asta 파일이 존재하지 않습니다: " + testFile.getAbsolutePath());

        // 일본어 노드 추출
        List<DiagramNode> nodes = astahParserService.extractJapaneseNodes(testFile);

        // 결과 검증
        assertNotNull(nodes, "추출된 노드 리스트가 null이면 안 됩니다");
        assertTrue(nodes.size() > 0, "최소 1개 이상의 일본어 노드가 추출되어야 합니다");

        // 타입별 통계
        Map<String, Long> typeStats = nodes.stream()
                .collect(Collectors.groupingBy(DiagramNode::getType, Collectors.counting()));

        // 통계 출력을 fail 메시지로 표시 (테스트 리포트에 나타나도록)
        StringBuilder report = new StringBuilder("\n\n=== 추출된 노드 통계 ===\n");
        report.append("총 노드 수: ").append(nodes.size()).append("\n\n");
        typeStats.forEach((type, count) -> {
            report.append(type).append(": ").append(count).append("개\n");
        });

        report.append("\n=== 샘플 노드 (각 타입당 최대 3개) ===\n");
        typeStats.keySet().forEach(type -> {
            report.append("\n[").append(type).append("]\n");
            nodes.stream()
                    .filter(node -> type.equals(node.getType()))
                    .limit(3)
                    .forEach(node -> report.append("  - ").append(node.getName()).append("\n"));
        });

        // 통계 출력
        System.out.println(report);

        // 예상 타입 검증
        assertTrue(typeStats.containsKey("diagram_name"),
                "다이어그램 이름이 추출되어야 합니다. 현재 타입: " + typeStats.keySet());
        assertTrue(typeStats.containsKey("mindmap_topic"),
                "마인드맵 토픽이 추출되어야 합니다. 현재 타입: " + typeStats.keySet());
        assertTrue(typeStats.containsKey("usecase"),
                "유스케이스가 추출되어야 합니다. 현재 타입: " + typeStats.keySet());

        // Activity Diagram 프레젠테이션 요소 검증
        boolean hasActivityPresentation = typeStats.keySet().stream()
                .anyMatch(type -> type.startsWith("presentation_"));
        assertTrue(hasActivityPresentation,
                "프레젠테이션 요소가 추출되어야 합니다. 현재 타입: " + typeStats.keySet());

        // 최소 개수 검증 (ASTAH_COMPONENTS_ANALYSIS.md 기준)
        // Activity Diagram 프레젠테이션: 191개
        // Mind Map 토픽: 32개
        // Use Case: 12개
        // 다이어그램 이름: 3개
        assertTrue(nodes.size() >= 200,
                "최소 200개 이상의 노드가 추출되어야 합니다 (현재: " + nodes.size() + "개). 통계:\n" + report);
    }

    @Test
    void testExtractDiagramNames() {
        File testFile = new File("../../test.asta");
        if (!testFile.exists()) {
            testFile = new File("../test.asta");
        }
        assertTrue(testFile.exists(), "test.asta 파일이 존재하지 않습니다");

        List<DiagramNode> nodes = astahParserService.extractJapaneseNodes(testFile);

        // 다이어그램 이름 추출 확인
        List<DiagramNode> diagramNames = nodes.stream()
                .filter(node -> "diagram_name".equals(node.getType()))
                .toList();

        assertFalse(diagramNames.isEmpty(), "다이어그램 이름이 추출되어야 합니다");

        System.out.println("\n=== 추출된 다이어그램 이름 ===");
        diagramNames.forEach(node -> {
            System.out.println("ID: " + node.getDiagramId());
            System.out.println("이름: " + node.getName());
            System.out.println();
        });
    }

    @Test
    void testExtractActivityDiagramPresentations() {
        File testFile = new File("../../test.asta");
        if (!testFile.exists()) {
            testFile = new File("../test.asta");
        }
        assertTrue(testFile.exists(), "test.asta 파일이 존재하지 않습니다");

        List<DiagramNode> nodes = astahParserService.extractJapaneseNodes(testFile);

        // Activity Diagram 프레젠테이션 요소 확인
        List<DiagramNode> activityPresentations = nodes.stream()
                .filter(node -> node.getType() != null && node.getType().startsWith("presentation_"))
                .toList();

        assertFalse(activityPresentations.isEmpty(), "Activity Diagram 프레젠테이션 요소가 추출되어야 합니다");

        System.out.println("\n=== Activity Diagram 프레젠테이션 요소 통계 ===");
        System.out.println("총 개수: " + activityPresentations.size());

        // 타입별 세부 통계
        Map<String, Long> presentationTypeStats = activityPresentations.stream()
                .collect(Collectors.groupingBy(DiagramNode::getType, Collectors.counting()));

        presentationTypeStats.forEach((type, count) -> {
            System.out.println(type + ": " + count + "개");
        });
    }
}
