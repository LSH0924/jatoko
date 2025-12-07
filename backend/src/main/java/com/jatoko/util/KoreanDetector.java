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
