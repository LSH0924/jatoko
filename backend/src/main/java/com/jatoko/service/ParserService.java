package com.jatoko.service;

import java.io.File;

/**
 * 파일 파싱 및 번역 적용을 위한 공통 인터페이스
 */
public interface ParserService {

    /**
     * 파일에서 텍스트를 추출하고, 번역하고, 다시 파일에 적용합니다.
     *
     * @param inputFile  원본 파일
     * @param outputFile 출력 파일
     */
    void extractTranslateAndApply(File inputFile, File outputFile);
}
