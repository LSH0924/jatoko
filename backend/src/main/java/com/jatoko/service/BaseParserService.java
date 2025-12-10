package com.jatoko.service;

import com.jatoko.model.NodeTranslation;
import com.jatoko.model.TranslationMetadata;
import com.jatoko.service.translator.Translator;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 파싱 및 번역 서비스의 공통 로직을 처리하는 추상 클래스
 *
 * @param <T> 노드 타입 (DiagramNode, SvgTextNode 등)
 */
@Slf4j
public abstract class BaseParserService<T> implements ParserService {

    protected final MetadataService metadataService;
    protected final Translator translator;

    protected BaseParserService(MetadataService metadataService, Translator translator) {
        this.metadataService = metadataService;
        this.translator = translator;
    }

    /**
     * 파일에서 노드를 추출합니다.
     */
    protected abstract List<T> extractNodes(File inputFile);

    /**
     * 번역된 결과를 파일에 적용합니다.
     */
    protected abstract void applyTranslationsInternal(File inputFile, List<T> nodes, File outputFile);

    // --- 추상 메서드: 노드 속성 접근자 ---
    protected abstract String getId(T node);
    protected abstract String getOriginalText(T node);
    protected abstract void setTranslatedText(T node, String text);
    
    /**
     * 노드가 중복인지 확인합니다. (기본값: false)
     * AstahParserService에서 오버라이드하여 사용합니다.
     */
    protected boolean isDuplicate(T node) {
        return false;
    }

    /**
     * 번역 후처리 (예: 중복 노드 처리)
     */
    protected void postProcessTranslations(List<T> allNodes, List<T> translatedNodes) {
        // 기본 구현은 아무것도 하지 않음
    }

    @Override
    public void extractTranslateAndApply(File inputFile, File outputFile) {
        extractTranslateAndApply(inputFile, outputFile, (msg, pct) -> {});
    }

    public void extractTranslateAndApply(File inputFile, File outputFile, ProgressCallback progressCallback) {
        try {
            log.info("통합 번역 시작: {}", inputFile.getName());
            progressCallback.onProgress("파일 분석 및 텍스트 추출 중...", 10);

            // 1. 노드 추출
            List<T> allNodes = extractNodes(inputFile);
            if (allNodes.isEmpty()) {
                log.warn("번역할 텍스트가 없습니다. 원본 파일을 복사합니다.");
                progressCallback.onProgress("번역할 텍스트 없음. 원본 복사 중...", 90);
                java.nio.file.Files.copy(inputFile.toPath(), outputFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                progressCallback.onProgress("완료", 100);
                return;
            }

            progressCallback.onProgress("메타데이터 로드 및 비교 중...", 20);

            // 2. 메타데이터 로드 및 비교
            TranslationMetadata metadata = metadataService.loadMetadata(inputFile);
            String currentHash = metadataService.calculateHash(inputFile);
            Map<String, NodeTranslation> previousTranslations = metadata.getTranslations();

            List<T> nodesToTranslate = new ArrayList<>();
            int reusedCount = 0;

            for (T node : allNodes) {
                if (isDuplicate(node)) continue;

                NodeTranslation prev = previousTranslations.get(getId(node));
                if (prev != null && prev.getOriginalText().equals(getOriginalText(node))) {
                    // 이전 번역 재사용
                    setTranslatedText(node, prev.getTranslatedText());
                    reusedCount++;
                } else {
                    // 번역 필요
                    nodesToTranslate.add(node);
                }
            }

            log.info("메타데이터 비교 결과: 재사용 {}개, 신규 번역 {}개", reusedCount, nodesToTranslate.size());

            // 3. 신규 번역 수행
            if (!nodesToTranslate.isEmpty()) {
                List<String> originalTexts = nodesToTranslate.stream()
                        .map(this::getOriginalText)
                        .collect(Collectors.toList());

                // 청크 단위로 번역 (DeepL API 제한 고려)
                final int CHUNK_SIZE = 50;
                List<String> translatedTexts = new ArrayList<>();
                int totalToTranslate = originalTexts.size();
                
                for (int i = 0; i < totalToTranslate; i += CHUNK_SIZE) {
                    if (i > 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    
                    // 진행률 계산 (20% ~ 80% 구간)
                    int currentProgress = 20 + (int)((double)i / totalToTranslate * 60);
                    progressCallback.onProgress(
                        String.format("DeepL 번역 중... (%d/%d)", i, totalToTranslate), 
                        currentProgress
                    );

                    int end = Math.min(i + CHUNK_SIZE, totalToTranslate);
                    List<String> chunk = originalTexts.subList(i, end);
                    translatedTexts.addAll(translator.translate(chunk));
                    log.info("번역 진행: {}/{}", translatedTexts.size(), totalToTranslate);
                }

                for (int i = 0; i < nodesToTranslate.size(); i++) {
                    setTranslatedText(nodesToTranslate.get(i), translatedTexts.get(i));
                }
            } else {
                progressCallback.onProgress("모든 텍스트가 이미 번역되어 있습니다.", 80);
            }

            progressCallback.onProgress("번역 후처리 중...", 85);
            // 4. 후처리 (중복 노드 처리 등)
            postProcessTranslations(allNodes, nodesToTranslate);

            // 5. 메타데이터 업데이트
            Map<String, NodeTranslation> newTranslations = new HashMap<>();
            
            // 다시 루프 돌면서 메타데이터 생성
            for (T node : allNodes) {
                 String translated = getTranslatedText(node);
                 if (translated != null) {
                     newTranslations.put(getId(node), NodeTranslation.builder()
                             .id(getId(node))
                             .originalText(getOriginalText(node))
                             .translatedText(translated)
                             .build());
                 }
            }

            metadata.setOriginalFileHash(currentHash);
            metadata.setLastModified(System.currentTimeMillis());
            metadata.setTranslations(newTranslations);
            metadataService.saveMetadata(inputFile, metadata);

            progressCallback.onProgress("파일 생성 및 적용 중...", 90);

            // 6. 번역 적용
            applyTranslationsInternal(inputFile, allNodes, outputFile);

            progressCallback.onProgress("완료", 100);
            log.info("통합 번역 완료: {} -> {}", inputFile.getName(), outputFile.getName());

        } catch (Exception e) {
            log.error("통합 번역 실패: {}", e.getMessage(), e);
            throw new RuntimeException("통합 번역 실패: " + e.getMessage(), e);
        }
    }
    
    // 추가 필요한 추상 메서드
    protected abstract String getTranslatedText(T node);
}
