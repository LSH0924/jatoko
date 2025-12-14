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
