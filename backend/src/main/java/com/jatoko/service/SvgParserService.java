package com.jatoko.service;

import com.jatoko.model.SvgTextNode;
import com.jatoko.service.svg.SvgDocumentLoader;
import com.jatoko.service.extractor.SvgTextExtractor;
import com.jatoko.service.applier.SvgTranslationApplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SVG 파일 파싱 및 번역 적용 서비스
 * Apache Batik을 사용하여 SVG 파일의 텍스트 요소를 추출하고 번역을 적용합니다.
 */
@Slf4j
@Service
public class SvgParserService extends BaseParserService<SvgTextNode> {

    private final SvgDocumentLoader documentLoader;
    private final SvgTextExtractor textExtractor;
    private final SvgTranslationApplier translationApplier;

    public SvgParserService(MetadataService metadataService,
                            SvgDocumentLoader documentLoader,
                            SvgTextExtractor textExtractor,
                            SvgTranslationApplier translationApplier,
                            com.jatoko.service.translator.Translator translator) {
        super(metadataService, translator);
        this.documentLoader = documentLoader;
        this.textExtractor = textExtractor;
        this.translationApplier = translationApplier;
    }

    @Override
    protected List<SvgTextNode> extractNodes(File inputFile) {
        return extractJapaneseTexts(inputFile);
    }

    @Override
    protected void applyTranslationsInternal(File inputFile, List<SvgTextNode> nodes, File outputFile) {
        // 번역 맵 생성 (id -> 번역 텍스트만)
        Map<String, String> translationMap = new HashMap<>();
        for (SvgTextNode node : nodes) {
            String id = node.getId();
            String translated = node.getTranslatedText();

            if (translated != null && !translated.isEmpty()) {
                // 번역 텍스트만 저장 (원문은 유지, 번역은 호버 시 오버레이로 표시)
                translationMap.put(id, translated);
            }
        }

        // 번역 적용
        applyTranslations(inputFile, translationMap, outputFile);
    }

    @Override
    protected String getId(SvgTextNode node) {
        return node.getId();
    }

    @Override
    protected String getOriginalText(SvgTextNode node) {
        return node.getOriginalText();
    }

    @Override
    protected void setTranslatedText(SvgTextNode node, String text) {
        node.setTranslatedText(text);
    }

    @Override
    protected String getTranslatedText(SvgTextNode node) {
        return node.getTranslatedText();
    }

    /**
     * SVG 파일에서 일본어 텍스트 노드를 추출합니다.
     * <text> 요소와 <foreignObject> 내부의 텍스트를 모두 처리합니다.
     *
     * @param svgFile SVG 파일
     * @return 일본어 텍스트 노드 리스트
     */
    public List<SvgTextNode> extractJapaneseTexts(File svgFile) {
        Document document = documentLoader.loadAndParse(svgFile);
        return textExtractor.extractJapaneseTexts(document);
    }

    /**
     * 번역을 SVG 파일에 적용합니다.
     * <text> 요소와 <foreignObject> 내부의 텍스트를 모두 처리합니다.
     *
     * @param svgFile 원본 SVG 파일
     * @param translations 번역 맵 (id -> translatedText)
     * @param outputFile 출력 파일
     */
    public void applyTranslations(File svgFile, Map<String, String> translations, File outputFile) {
        Document document = documentLoader.loadAndParse(svgFile);
        int appliedCount = translationApplier.applyTranslations(document, translations);
        documentLoader.save(document, outputFile);

        log.info("번역 적용 완료: {}개 텍스트 노드, 출력 파일: {}",
                 appliedCount, outputFile.getAbsolutePath());
    }

}
