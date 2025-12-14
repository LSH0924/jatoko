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

import com.jatoko.model.SvgTextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SvgParserService 테스트
 * BaseParserService를 상속받은 SVG 파일 파싱 서비스를 테스트합니다.
 */
@SpringBootTest
class SvgParserServiceTest {

    @Autowired
    private SvgParserService svgParserService;

    @TempDir
    Path tempDir;

    @Test
    void testExtractJapaneseTexts() throws Exception {
        // 테스트용 SVG 파일 생성
        String svgContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300">
                    <text id="text1" x="10" y="20" font-size="14">こんにちは</text>
                    <text id="text2" x="10" y="40" font-size="14">Hello World</text>
                    <text id="text3" x="10" y="60" font-size="14">日本語テキスト</text>
                    <foreignObject x="10" y="80" width="200" height="100">
                        <div xmlns="http://www.w3.org/1999/xhtml">
                            <p>これはテストです</p>
                        </div>
                    </foreignObject>
                </svg>
                """;

        File svgFile = tempDir.resolve("test.svg").toFile();
        Files.writeString(svgFile.toPath(), svgContent);

        // 일본어 텍스트 추출
        List<SvgTextNode> nodes = svgParserService.extractJapaneseTexts(svgFile);

        // 검증
        assertNotNull(nodes, "추출된 노드 리스트가 null이면 안 됩니다");
        assertTrue(nodes.size() >= 2, "최소 2개 이상의 일본어 텍스트가 추출되어야 합니다");

        // 통계 출력
        System.out.println("\n=== 추출된 SVG 텍스트 노드 ===");
        System.out.println("총 노드 수: " + nodes.size());
        nodes.forEach(node -> {
            System.out.println("ID: " + node.getId());
            System.out.println("원본: " + node.getOriginalText());
            System.out.println("위치: (" + node.getX() + ", " + node.getY() + ")");
            System.out.println();
        });

        // 특정 텍스트 확인
        boolean hasKonnichiwa = nodes.stream()
                .anyMatch(node -> node.getOriginalText().contains("こんにちは"));
        assertTrue(hasKonnichiwa, "こんにちは 텍스트가 추출되어야 합니다");

        boolean hasNihongo = nodes.stream()
                .anyMatch(node -> node.getOriginalText().contains("日本語"));
        assertTrue(hasNihongo, "日本語 텍스트가 추출되어야 합니다");
    }

    @Test
    void testExtractJapaneseTexts_EmptySvg() throws Exception {
        // 일본어가 없는 SVG 파일
        String svgContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300">
                    <text id="text1" x="10" y="20">Hello World</text>
                    <text id="text2" x="10" y="40">English Only</text>
                </svg>
                """;

        File svgFile = tempDir.resolve("empty.svg").toFile();
        Files.writeString(svgFile.toPath(), svgContent);

        // 일본어 텍스트 추출
        List<SvgTextNode> nodes = svgParserService.extractJapaneseTexts(svgFile);

        // 검증
        assertNotNull(nodes, "추출된 노드 리스트가 null이면 안 됩니다");
        assertEquals(0, nodes.size(), "일본어가 없는 경우 빈 리스트가 반환되어야 합니다");
    }

    @Test
    void testExtractTranslateAndApply() throws Exception {
        // 테스트용 SVG 파일 생성
        String svgContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300">
                    <text id="text1" x="10" y="20" font-size="14">こんにちは</text>
                    <text id="text2" x="10" y="40" font-size="14">日本語</text>
                </svg>
                """;

        File svgFile = tempDir.resolve("input.svg").toFile();
        Files.writeString(svgFile.toPath(), svgContent);

        File outputFile = tempDir.resolve("output.svg").toFile();

        // DeepL API 키 확인
        String deeplApiKey = System.getenv("DEEPL_API_KEY");
        if (deeplApiKey == null || deeplApiKey.isEmpty() || deeplApiKey.equals("your-api-key-here")) {
            System.out.println("DEEPL_API_KEY가 설정되지 않아 통합 번역 테스트를 스킵합니다.");
            return;
        }

        // 통합 번역 실행
        assertDoesNotThrow(() -> {
            svgParserService.extractTranslateAndApply(svgFile, outputFile);
        });

        // 출력 파일 생성 확인
        assertTrue(outputFile.exists(), "번역된 파일이 생성되어야 합니다");
        assertTrue(outputFile.length() > 0, "번역된 파일의 크기가 0보다 커야 합니다");

        // 메타데이터 파일 생성 확인
        File metadataFile = new File(svgFile.getAbsolutePath() + ".meta.json");
        assertTrue(metadataFile.exists(), "메타데이터 파일이 생성되어야 합니다");

        // 출력 파일이 유효한 SVG인지 확인
        String outputContent = Files.readString(outputFile.toPath());
        assertTrue(outputContent.contains("<svg"), "출력 파일이 유효한 SVG여야 합니다");
        assertTrue(outputContent.contains("</svg>"), "출력 파일이 유효한 SVG여야 합니다");
    }

    @Test
    void testNodeProperties() {
        // SvgTextNode의 속성 테스트
        SvgTextNode node = new SvgTextNode();
        node.setId("test-id");
        node.setOriginalText("テスト");
        node.setTranslatedText("테스트");
        node.setX("10");
        node.setY("20");
        node.setFontSize("14");
        node.setFontFamily("Arial");

        assertEquals("test-id", node.getId());
        assertEquals("テスト", node.getOriginalText());
        assertEquals("테스트", node.getTranslatedText());
        assertEquals("10", node.getX());
        assertEquals("20", node.getY());
        assertEquals("14", node.getFontSize());
        assertEquals("Arial", node.getFontFamily());
    }
}
