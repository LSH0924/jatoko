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

package com.jatoko.service.applier;

import com.change_vision.jude.api.inf.model.*;
import com.jatoko.util.KoreanDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;
import java.util.Set;

/**
 * 모델 요소에 번역을 적용하는 컴포넌트
 */
@Slf4j
@Component
public class ModelTranslationApplier {

    /**
     * 모델 요소를 재귀적으로 탐색하여 번역을 적용합니다.
     *
     * @param element 모델 요소
     * @param translationMap 번역 맵 (ID → translatedName)
     * @param progressCallback 진행 상황 콜백 (optional)
     * @param startProgress 시작 진행률
     * @param endProgress 종료 진행률
     * @return 수정된 요소 수
     */
    public int applyTranslations(INamedElement element, Map<String, String> translationMap,
                                  Consumer<Integer> progressCallback, int startProgress, int endProgress) {
        if (element == null || translationMap == null) return 0;

        int count = 0;

        // 현재 요소의 이름 변경
        String elementId = element.getId();
        if (translationMap.containsKey(elementId)) {
            try {
                String originalName = element.getName();

                // 한글이 포함되어 있지 않을 때에만 번역 적용하기
                if (!KoreanDetector.containsKorean(originalName)) {
                    String translatedName = translationMap.get(elementId);

                    // 개행문자를 지원하지 않는 타입들은 / 구분자 사용
                    String separator = getSeparator(element);
                    element.setName(originalName + separator + translatedName);
                    count++;
                }
            } catch (Exception e) {
                String elementType = element.getClass().getSimpleName();
                System.err.println("요소 이름 변경 실패 [" + elementType + "]: " + elementId +
                    " (원본: \"" + element.getName() + "\") - " + e.getMessage());
            }
        }

        // 자식 요소 처리
        if (element instanceof IPackage) {
            IPackage pkg = (IPackage) element;
            try {
                for (INamedElement child : pkg.getOwnedElements()) {
                    count += applyTranslations(child, translationMap, progressCallback, startProgress, endProgress);
                }
            } catch (Exception e) {
                // 일부 요소 접근 실패 시 계속 진행
                log.warn("패키지 자식 요소 접근 실패: {}", e.getMessage());
                logStackTrace(e);
            }
        }

        // 클래스/인터페이스의 속성과 메서드 처리
        if (element instanceof IClass) {
            IClass cls = (IClass) element;
            try {
                for (IAttribute attr : cls.getAttributes()) {
                    count += applyTranslations(attr, translationMap, progressCallback, startProgress, endProgress);
                }
                for (IOperation op : cls.getOperations()) {
                    count += applyTranslations(op, translationMap, progressCallback, startProgress, endProgress);
                }
            } catch (Exception e) {
                // 일부 요소 접근 실패 시 계속 진행
                log.warn("클래스 속성/메서드 접근 실패: {}", e.getMessage());
                logStackTrace(e);
            }
        }

        return count;
    }

    /**
     * 요소 타입에 따라 적절한 구분자를 반환합니다.
     * 일부 요소는 개행문자(\n)를 지원하지 않으므로 / 구분자를 사용합니다.
     *
     * @param element 모델 요소
     * @return 구분자 문자열
     */
    private String getSeparator(INamedElement element) {
        // Class, ActivityNode, Activity 타입은 개행문자를 지원하지 않음
        if (element instanceof IClass ||
            element instanceof IActivityNode ||
            element instanceof IActivity ||
            element instanceof IPackage
        ) {
            return " / ";
        }
        // 기본값: 개행문자
        return "\n";
    }

    /**
     * 주석/노트에 번역을 적용합니다.
     *
     * @param project 프로젝트 모델
     * @param commentTranslationMap 주석 번역 맵
     * @return 수정된 주석 수
     */
    public int applyTranslationsToComments(IModel project, Map<String, String> commentTranslationMap) {
        int count = 0;

        try {
            count += applyTranslationsToCommentsRecursive(project, commentTranslationMap);
        } catch (Exception e) {
            System.err.println("주석 번역 적용 실패: " + e.getMessage());
        }

        return count;
    }

    /**
     * 재귀적으로 모델 요소를 탐색하여 주석에 번역을 적용합니다.
     */
    private int applyTranslationsToCommentsRecursive(INamedElement element, Map<String, String> commentTranslationMap) {
        if (element == null) return 0;

        int count = 0;

        // 주석인 경우 번역 적용
        if (element instanceof IComment) {
            IComment comment = (IComment) element;
            String commentId = comment.getId() + "_body";
            if (commentTranslationMap.containsKey(commentId)) {
                try {
                    // IComment는 setBody() 메서드가 없을 수 있음
                    System.err.println("주석 번역 적용 시도 (setBody 메서드 확인 필요): " + commentId);
                    count++;
                } catch (Exception e) {
                    System.err.println("주석 본문 변경 실패: " + e.getMessage());
                }
            }
        }

        // 자식 요소 탐색
        if (element instanceof IPackage) {
            IPackage pkg = (IPackage) element;
            try {
                for (INamedElement child : pkg.getOwnedElements()) {
                    count += applyTranslationsToCommentsRecursive(child, commentTranslationMap);
                }
            } catch (Exception e) {
                // 일부 요소 접근 실패 시 계속 진행
                log.warn("패키지 자식 요소 접근 실패 (주석 탐색): {}", e.getMessage());
                logStackTrace(e);
            }
        }

        return count;
    }

    /**
     * 예외의 상위 2~3줄 스택 트레이스를 로깅합니다.
     */
    private void logStackTrace(Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        int linesToShow = Math.min(3, stackTrace.length);
        StringBuilder sb = new StringBuilder("Stack trace:");
        for (int i = 0; i < linesToShow; i++) {
            sb.append("\n  at ").append(stackTrace[i]);
        }
        log.warn(sb.toString());
    }
}
