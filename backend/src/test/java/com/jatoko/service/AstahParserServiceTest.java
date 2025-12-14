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

import com.jatoko.model.DiagramNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AstahParserService 테스트
 * BaseParserService를 상속받은 Astah 파일 파싱 서비스를 테스트합니다.
 */
@SpringBootTest
class AstahParserServiceTest {

    @Autowired
    private AstahParserService astahParserService;

    @TempDir
    Path tempDir;

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

        // 프레젠테이션 요소 검증
        boolean hasPresentation = typeStats.keySet().stream()
                .anyMatch(type -> type.startsWith("presentation_"));
        assertTrue(hasPresentation,
                "프레젠테이션 요소가 추출되어야 합니다. 현재 타입: " + typeStats.keySet());

        // 최소 개수 검증 (현재 test.asta 파일 기준)
        // 다이어그램 이름, 마인드맵 토픽, 프레젠테이션 요소 등
        assertTrue(nodes.size() >= 10,
                "최소 10개 이상의 노드가 추출되어야 합니다 (현재: " + nodes.size() + "개). 통계:\n" + report);
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

    @Test
    void testDuplicateNodeHandling() {
        File testFile = new File("../../test.asta");
        if (!testFile.exists()) {
            testFile = new File("../test.asta");
        }
        assertTrue(testFile.exists(), "test.asta 파일이 존재하지 않습니다");

        List<DiagramNode> nodes = astahParserService.extractJapaneseNodes(testFile);

        // 중복 노드 확인
        List<DiagramNode> duplicateNodes = nodes.stream()
                .filter(DiagramNode::isDuplicate)
                .toList();

        System.out.println("\n=== 중복 노드 통계 ===");
        System.out.println("총 노드 수: " + nodes.size());
        System.out.println("중복 노드 수: " + duplicateNodes.size());
        System.out.println("고유 노드 수: " + (nodes.size() - duplicateNodes.size()));

        // 중복 노드가 있는 경우 통계 출력
        if (!duplicateNodes.isEmpty()) {
            Map<String, Long> duplicateStats = duplicateNodes.stream()
                    .collect(Collectors.groupingBy(DiagramNode::getName, Collectors.counting()));

            System.out.println("\n중복된 이름 (상위 5개):");
            duplicateStats.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .limit(5)
                    .forEach(entry -> {
                        System.out.println("  " + entry.getKey() + ": " + entry.getValue() + "개");
                    });
        }
    }

    @Test
    void testExtractTranslateAndApply() {
        File testFile = new File("../../test.asta");
        if (!testFile.exists()) {
            testFile = new File("../test.asta");
        }
        if (!testFile.exists()) {
            testFile = new File("test.asta");
        }
        assertTrue(testFile.exists(), "test.asta 파일이 존재하지 않습니다: " + testFile.getAbsolutePath());

        // 임시 출력 파일
        File outputFile = tempDir.resolve("translated.asta").toFile();

        // 통합 번역 테스트 (실제 번역은 수행하지 않고 구조 테스트)
        // 주의: 이 테스트는 DeepL API 키가 설정되어 있어야 실행됩니다
        // CI/CD 환경에서는 스킵할 수 있습니다
        String deeplApiKey = System.getenv("DEEPL_API_KEY");
        if (deeplApiKey == null || deeplApiKey.isEmpty() || deeplApiKey.equals("your-api-key-here")) {
            System.out.println("DEEPL_API_KEY가 설정되지 않아 통합 번역 테스트를 스킵합니다.");
            return;
        }

        // 통합 번역 실행
        final File inputFile = testFile;
        final File output = outputFile;
        assertDoesNotThrow(() -> {
            astahParserService.extractTranslateAndApply(inputFile, output);
        });

        // 출력 파일 생성 확인
        assertTrue(outputFile.exists(), "번역된 파일이 생성되어야 합니다");
        assertTrue(outputFile.length() > 0, "번역된 파일의 크기가 0보다 커야 합니다");

        // 메타데이터 파일 생성 확인
        File metadataFile = new File(testFile.getAbsolutePath() + ".meta.json");
        assertTrue(metadataFile.exists(), "메타데이터 파일이 생성되어야 합니다");
    }
}
