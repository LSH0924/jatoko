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

package com.jatoko.util;

public class KoreanDetector {

    /**
     * 문자열에 한글 문자가 포함되어 있는지 확인합니다.
     *
     * @param text 확인할 문자열
     * @return 한글 문자가 포함되어 있으면 true, 아니면 false
     */
    public static boolean containsKorean(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        for (char c : text.toCharArray()) {
            if (isKoreanCharacter(c)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 개별 문자가 한글 문자인지 확인합니다.
     *
     * @param c 확인할 문자
     * @return 한글 문자이면 true, 아니면 false
     */
    public static boolean isKoreanCharacter(char c) {
        // 한글 음절: U+AC00 - U+D7A3 (가-힣)
        if (c >= 0xAC00 && c <= 0xD7A3) {
            return true;
        }

        // 한글 자모 (초성, 중성, 종성): U+1100 - U+11FF
        if (c >= 0x1100 && c <= 0x11FF) {
            return true;
        }

        // 한글 호환 자모: U+3130 - U+318F (ㄱ-ㆎ)
        if (c >= 0x3130 && c <= 0x318F) {
            return true;
        }

        // 반각 한글: U+FFA0 - U+FFDC
        if (c >= 0xFFA0 && c <= 0xFFDC) {
            return true;
        }

        return false;
    }
}
