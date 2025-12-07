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
        try {
            log.info("통합 번역 시작: {}", inputFile.getName());

            // 1. 노드 추출
            List<T> allNodes = extractNodes(inputFile);
            if (allNodes.isEmpty()) {
                log.warn("번역할 텍스트가 없습니다. 원본 파일을 복사합니다.");
                java.nio.file.Files.copy(inputFile.toPath(), outputFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return;
            }

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
                
                for (int i = 0; i < originalTexts.size(); i += CHUNK_SIZE) {
                    if (i > 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    int end = Math.min(i + CHUNK_SIZE, originalTexts.size());
                    List<String> chunk = originalTexts.subList(i, end);
                    translatedTexts.addAll(translator.translate(chunk));
                    log.info("번역 진행: {}/{}", translatedTexts.size(), originalTexts.size());
                }

                for (int i = 0; i < nodesToTranslate.size(); i++) {
                    setTranslatedText(nodesToTranslate.get(i), translatedTexts.get(i));
                }
            }

            // 4. 후처리 (중복 노드 처리 등)
            postProcessTranslations(allNodes, nodesToTranslate);

            // 5. 메타데이터 업데이트
            Map<String, NodeTranslation> newTranslations = new HashMap<>();
            for (T node : allNodes) {
                // 번역된 텍스트가 있는 경우에만 메타데이터에 저장
                // (중복 노드도 번역된 텍스트가 설정되어 있으면 저장됨)
                // 주의: getId(node)가 중복될 경우 덮어씌워짐. Astah의 경우 ID가 고유하므로 문제 없음.
                // Svg의 경우도 ID가 고유함.
                String translated = null;
                // getTranslatedText가 protected라서 직접 호출 불가? 아니, 같은 클래스 내라 가능하지만
                // T 타입에는 메서드가 없으므로 추상 메서드 사용해야 함.
                // 하지만 getter 추상 메서드를 안 만들었네?
                // setTranslatedText만 만들었음.
                // -> getTranslatedText도 필요함.
                // 하지만 여기서는 이미 setTranslatedText로 설정된 값을 가져와야 하는데...
                // T 객체 자체에 값이 설정되어 있다고 가정함.
                // getTranslatedText 추상 메서드 추가 필요.
            }
            
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

            // 6. 번역 적용
            applyTranslationsInternal(inputFile, allNodes, outputFile);

            log.info("통합 번역 완료: {} -> {}", inputFile.getName(), outputFile.getName());

        } catch (Exception e) {
            log.error("통합 번역 실패: {}", e.getMessage(), e);
            throw new RuntimeException("통합 번역 실패: " + e.getMessage(), e);
        }
    }
    
    // 추가 필요한 추상 메서드
    protected abstract String getTranslatedText(T node);
}
