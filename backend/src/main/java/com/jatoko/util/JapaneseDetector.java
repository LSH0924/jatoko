package com.jatoko.util;

public class JapaneseDetector {

    /**
     * 문자열에 일본어 문자(히라가나, 가타카나, 한자)가 포함되어 있는지 확인합니다.
     *
     * @param text 확인할 문자열
     * @return 일본어 문자가 포함되어 있으면 true, 아니면 false
     */
    public static boolean containsJapanese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        for (char c : text.toCharArray()) {
            if (isJapaneseCharacter(c)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 개별 문자가 일본어 문자인지 확인합니다.
     *
     * @param c 확인할 문자
     * @return 일본어 문자이면 true, 아니면 false
     */
    public static boolean isJapaneseCharacter(char c) {
        // 히라가나: U+3040 - U+309F
        if (c >= 0x3040 && c <= 0x309F) {
            return true;
        }

        // 가타카나: U+30A0 - U+30FF
        if (c >= 0x30A0 && c <= 0x30FF) {
            return true;
        }

        // CJK 통합 한자 (Kanji): U+4E00 - U+9FAF
        // 주의: 이 범위는 중국어 간체/번체와 겹칩니다.
        // 일본어 전용 한자를 정확히 구분하기는 어렵습니다.
        if (c >= 0x4E00 && c <= 0x9FAF) {
            return true;
        }

        // 히라가나 음성 기호: U+3099 - U+309C
        if (c >= 0x3099 && c <= 0x309C) {
            return true;
        }

        // 가타카나 음성 확장: U+31F0 - U+31FF
        if (c >= 0x31F0 && c <= 0x31FF) {
            return true;
        }

        // 반각 가타카나: U+FF65 - U+FF9F
        if (c >= 0xFF65 && c <= 0xFF9F) {
            return true;
        }

        return false;
    }

    /**
     * 문자열이 주로 일본어로 구성되어 있는지 확인합니다.
     *
     * @param text 확인할 문자열
     * @param threshold 일본어 문자 비율 임계값 (0.0 ~ 1.0)
     * @return 일본어 문자 비율이 임계값 이상이면 true
     */
    public static boolean isPrimarilyJapanese(String text, double threshold) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        int totalChars = 0;
        int japaneseChars = 0;

        for (char c : text.toCharArray()) {
            // 공백, 구두점 등은 제외
            if (!Character.isWhitespace(c) && !Character.isISOControl(c)) {
                totalChars++;
                if (isJapaneseCharacter(c)) {
                    japaneseChars++;
                }
            }
        }

        if (totalChars == 0) {
            return false;
        }

        double ratio = (double) japaneseChars / totalChars;
        return ratio >= threshold;
    }
}
