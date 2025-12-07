package com.jatoko.service;

import com.jatoko.model.NodeTranslation;
import com.jatoko.model.TranslationMetadata;
import com.jatoko.service.translator.Translator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BaseParserService 테스트
 * 템플릿 메서드 패턴으로 구현된 공통 로직을 테스트합니다.
 */
class BaseParserServiceTest {

    @Mock
    private MetadataService metadataService;

    @Mock
    private Translator translator;

    private TestParserService testParserService;

    @TempDir
    Path tempDir;

    /**
     * 테스트용 노드 클래스
     */
    static class TestNode {
        private String id;
        private String originalText;
        private String translatedText;
        private boolean duplicate;

        public TestNode(String id, String originalText) {
            this.id = id;
            this.originalText = originalText;
            this.duplicate = false;
        }

        public String getId() {
            return id;
        }

        public String getOriginalText() {
            return originalText;
        }

        public String getTranslatedText() {
            return translatedText;
        }

        public void setTranslatedText(String translatedText) {
            this.translatedText = translatedText;
        }

        public boolean isDuplicate() {
            return duplicate;
        }

        public void setDuplicate(boolean duplicate) {
            this.duplicate = duplicate;
        }
    }

    /**
     * 테스트용 BaseParserService 구현
     */
    static class TestParserService extends BaseParserService<TestNode> {

        private List<TestNode> nodesToExtract;
        private boolean applyTranslationsCalled = false;

        public TestParserService(MetadataService metadataService, Translator translator) {
            super(metadataService, translator);
            this.nodesToExtract = new ArrayList<>();
        }

        public void setNodesToExtract(List<TestNode> nodes) {
            this.nodesToExtract = nodes;
        }

        @Override
        protected List<TestNode> extractNodes(File inputFile) {
            return nodesToExtract;
        }

        @Override
        protected void applyTranslationsInternal(File inputFile, List<TestNode> nodes, File outputFile) {
            applyTranslationsCalled = true;
            try {
                Files.writeString(outputFile.toPath(), "translated content");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected String getId(TestNode node) {
            return node.getId();
        }

        @Override
        protected String getOriginalText(TestNode node) {
            return node.getOriginalText();
        }

        @Override
        protected void setTranslatedText(TestNode node, String text) {
            node.setTranslatedText(text);
        }

        @Override
        protected String getTranslatedText(TestNode node) {
            return node.getTranslatedText();
        }

        @Override
        protected boolean isDuplicate(TestNode node) {
            return node.isDuplicate();
        }

        public boolean isApplyTranslationsCalled() {
            return applyTranslationsCalled;
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testParserService = new TestParserService(metadataService, translator);
    }

    @Test
    void testExtractTranslateAndApply_NewTranslation() throws Exception {
        // 테스트 파일 생성
        File inputFile = tempDir.resolve("input.txt").toFile();
        File outputFile = tempDir.resolve("output.txt").toFile();
        Files.writeString(inputFile.toPath(), "test content");

        // 추출할 노드 설정
        List<TestNode> nodes = Arrays.asList(
                new TestNode("id1", "こんにちは"),
                new TestNode("id2", "日本語")
        );
        testParserService.setNodesToExtract(nodes);

        // 메타데이터 모킹
        TranslationMetadata metadata = new TranslationMetadata();
        metadata.setTranslations(new HashMap<>());
        when(metadataService.loadMetadata(any(File.class))).thenReturn(metadata);
        when(metadataService.calculateHash(any(File.class))).thenReturn("hash123");

        // 번역 모킹
        when(translator.translate(anyList())).thenReturn(Arrays.asList("안녕하세요", "일본어"));

        // 실행
        testParserService.extractTranslateAndApply(inputFile, outputFile);

        // 검증
        assertTrue(testParserService.isApplyTranslationsCalled(), "applyTranslationsInternal이 호출되어야 합니다");
        assertTrue(outputFile.exists(), "출력 파일이 생성되어야 합니다");

        // 번역이 노드에 설정되었는지 확인
        assertEquals("안녕하세요", nodes.get(0).getTranslatedText());
        assertEquals("일본어", nodes.get(1).getTranslatedText());

        // 메타데이터 저장 호출 확인
        verify(metadataService, times(1)).saveMetadata(eq(inputFile), any(TranslationMetadata.class));
    }

    @Test
    void testExtractTranslateAndApply_ReuseTranslation() throws Exception {
        // 테스트 파일 생성
        File inputFile = tempDir.resolve("input.txt").toFile();
        File outputFile = tempDir.resolve("output.txt").toFile();
        Files.writeString(inputFile.toPath(), "test content");

        // 추출할 노드 설정
        List<TestNode> nodes = Arrays.asList(
                new TestNode("id1", "こんにちは"),
                new TestNode("id2", "日本語")
        );
        testParserService.setNodesToExtract(nodes);

        // 기존 번역이 있는 메타데이터 모킹
        TranslationMetadata metadata = new TranslationMetadata();
        Map<String, NodeTranslation> previousTranslations = new HashMap<>();
        previousTranslations.put("id1", NodeTranslation.builder()
                .id("id1")
                .originalText("こんにちは")
                .translatedText("안녕하세요 (재사용)")
                .build());
        metadata.setTranslations(previousTranslations);

        when(metadataService.loadMetadata(any(File.class))).thenReturn(metadata);
        when(metadataService.calculateHash(any(File.class))).thenReturn("hash123");

        // 번역 모킹 (id2만 번역됨)
        when(translator.translate(anyList())).thenReturn(Collections.singletonList("일본어 (신규)"));

        // 실행
        testParserService.extractTranslateAndApply(inputFile, outputFile);

        // 검증
        assertEquals("안녕하세요 (재사용)", nodes.get(0).getTranslatedText(), "기존 번역이 재사용되어야 합니다");
        assertEquals("일본어 (신규)", nodes.get(1).getTranslatedText(), "새로운 텍스트는 번역되어야 합니다");

        // 번역 API는 1개 항목만 호출되어야 함
        verify(translator, times(1)).translate(argThat(list -> list.size() == 1));
    }

    @Test
    void testExtractTranslateAndApply_EmptyNodes() throws Exception {
        // 테스트 파일 생성
        File inputFile = tempDir.resolve("input.txt").toFile();
        File outputFile = tempDir.resolve("output.txt").toFile();
        Files.writeString(inputFile.toPath(), "test content");

        // 빈 노드 리스트
        testParserService.setNodesToExtract(Collections.emptyList());

        // 실행
        testParserService.extractTranslateAndApply(inputFile, outputFile);

        // 검증
        assertTrue(outputFile.exists(), "출력 파일이 생성되어야 합니다");

        // 번역 API는 호출되지 않아야 함
        verify(translator, never()).translate(anyList());

        // applyTranslationsInternal은 호출되지 않아야 함
        assertFalse(testParserService.isApplyTranslationsCalled(), "노드가 없으면 applyTranslationsInternal이 호출되지 않아야 합니다");
    }

    @Test
    void testExtractTranslateAndApply_DuplicateNodes() throws Exception {
        // 테스트 파일 생성
        File inputFile = tempDir.resolve("input.txt").toFile();
        File outputFile = tempDir.resolve("output.txt").toFile();
        Files.writeString(inputFile.toPath(), "test content");

        // 중복 노드 포함
        TestNode node1 = new TestNode("id1", "こんにちは");
        TestNode node2 = new TestNode("id2", "こんにちは");
        node2.setDuplicate(true);  // 중복 표시

        List<TestNode> nodes = Arrays.asList(node1, node2);
        testParserService.setNodesToExtract(nodes);

        // 메타데이터 모킹
        TranslationMetadata metadata = new TranslationMetadata();
        metadata.setTranslations(new HashMap<>());
        when(metadataService.loadMetadata(any(File.class))).thenReturn(metadata);
        when(metadataService.calculateHash(any(File.class))).thenReturn("hash123");

        // 번역 모킹
        when(translator.translate(anyList())).thenReturn(Collections.singletonList("안녕하세요"));

        // 실행
        testParserService.extractTranslateAndApply(inputFile, outputFile);

        // 검증
        assertEquals("안녕하세요", node1.getTranslatedText(), "대표 노드는 번역되어야 합니다");
        assertNull(node2.getTranslatedText(), "중복 노드는 번역 API 호출에서 제외되어야 합니다");

        // 번역 API는 1개 항목만 호출되어야 함
        verify(translator, times(1)).translate(argThat(list -> list.size() == 1));
    }
}
