package com.jatoko.service.svg;

import lombok.extern.slf4j.Slf4j;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * SVG 문서 로딩 및 저장을 담당하는 클래스
 * Apache Batik을 사용하여 SVG 파일을 파싱하고 저장합니다.
 */
@Slf4j
@Component
public class SvgDocumentLoader {

    /**
     * SVG 파일을 로드하고 파싱합니다.
     * feDropShadow 요소를 전처리하여 제거한 후 파싱합니다.
     *
     * @param svgFile SVG 파일
     * @return 파싱된 SVG Document 객체
     */
    public Document loadAndParse(File svgFile) {
        try {
            // SVG 파일을 읽어서 feDropShadow 전처리
            String svgContent = Files.readString(svgFile.toPath(), StandardCharsets.UTF_8);
            svgContent = preprocessFeDropShadow(svgContent);

            // 전처리된 SVG를 임시 파일로 저장
            File tempFile = File.createTempFile("svg_preprocessed_", ".svg");
            tempFile.deleteOnExit();
            Files.writeString(tempFile.toPath(), svgContent, StandardCharsets.UTF_8);

            // SVG 문서 파싱
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            Document document = factory.createDocument(tempFile.toURI().toString());

            log.debug("SVG 파일 로드 및 파싱 완료: {}", svgFile.getAbsolutePath());
            return document;

        } catch (Exception e) {
            log.error("SVG 파일 로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("SVG 파일 로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * SVG Document를 파일로 저장합니다.
     *
     * @param document SVG Document 객체
     * @param outputFile 출력 파일
     */
    public void save(Document document, File outputFile) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new FileOutputStream(outputFile));
            transformer.transform(source, result);

            log.debug("SVG 파일 저장 완료: {}", outputFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("SVG 파일 저장 실패: {}", e.getMessage(), e);
            throw new RuntimeException("SVG 파일 저장 실패: " + e.getMessage(), e);
        }
    }

    /**
     * SVG 파일 내용에서 feDropShadow 요소를 제거합니다.
     * Apache Batik은 SVG 2.0의 feDropShadow를 지원하지 않으므로,
     * 파싱 전에 문자열 치환으로 제거합니다.
     *
     * @param svgContent SVG 파일 내용
     * @return 전처리된 SVG 내용
     */
    private String preprocessFeDropShadow(String svgContent) {
        // feDropShadow 요소를 빈 문자열로 대체
        // 정규식: <feDropShadow ... /> 또는 <feDropShadow ...></feDropShadow>
        String processed = svgContent.replaceAll("<feDropShadow[^>]*/>", "");
        processed = processed.replaceAll("<feDropShadow[^>]*>.*?</feDropShadow>", "");

        if (!processed.equals(svgContent)) {
            log.info("SVG 파일에서 feDropShadow 요소 제거 완료 (Batik SVG 1.1 호환성)");
        }

        return processed;
    }
}
