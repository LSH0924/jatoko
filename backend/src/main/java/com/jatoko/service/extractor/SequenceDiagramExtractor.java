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

package com.jatoko.service.extractor;

import com.change_vision.jude.api.inf.model.*;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.jatoko.model.DiagramNode;
import com.jatoko.util.JapaneseDetector;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 시퀀스 다이어그램 추출기
 */
@Component
@Order(2)
public class SequenceDiagramExtractor extends BaseExtractor implements DiagramExtractor {

    @Override
    public boolean supports(IDiagram diagram) {
        return diagram instanceof ISequenceDiagram;
    }

    @Override
    public void extract(IDiagram diagram, List<DiagramNode> japaneseNodes) {
        if (!(diagram instanceof ISequenceDiagram)) {
            return;
        }

        ISequenceDiagram sequenceDiagram = (ISequenceDiagram) diagram;

        try {
            IInteraction interaction = sequenceDiagram.getInteraction();
            if (interaction != null) {
                // 인터랙션 이름
                extractFromNamedElement(interaction, japaneseNodes);

                // 라이프라인 추출
                ILifeline[] lifelines = interaction.getLifelines();
                for (ILifeline lifeline : lifelines) {
                    extractFromNamedElement(lifeline, japaneseNodes);
                }

                // 메시지 추출
                IMessage[] messages = interaction.getMessages();
                for (IMessage message : messages) {
                    extractFromMessage(message, japaneseNodes);
                }
            }

            // 프레젠테이션 요소도 추출
            IPresentation[] presentations = sequenceDiagram.getPresentations();
            for (IPresentation presentation : presentations) {
                extractFromPresentation(presentation, japaneseNodes);
            }
        } catch (Exception e) {
            System.err.println("시퀀스 다이어그램 추출 실패: " + e.getMessage());
        }
    }

    @Override
    public int applyTranslations(IDiagram diagram, Map<String, String> translationMap) {
        if (!(diagram instanceof ISequenceDiagram)) {
            return 0;
        }

        ISequenceDiagram sequenceDiagram = (ISequenceDiagram) diagram;
        int count = 0;

        try {
            IInteraction interaction = sequenceDiagram.getInteraction();
            if (interaction != null) {
                IMessage[] messages = interaction.getMessages();
                for (IMessage message : messages) {
                    count += applyTranslationsToMessage(message, translationMap);
                }
            }
        } catch (Exception e) {
            System.err.println("시퀀스 다이어그램 번역 적용 실패: " + e.getMessage());
        }

        return count;
    }

    /**
     * 네임드 엘리먼트에서 일본어를 추출합니다.
     */
    private void extractFromNamedElement(INamedElement element, List<DiagramNode> japaneseNodes) {
        try {
            String name = element.getName();
            if (name != null && !name.isEmpty() && JapaneseDetector.containsJapanese(name)) {
                japaneseNodes.add(DiagramNode.builder()
                        .id(element.getId())
                        .name(name)
                        .type(getElementType(element))
                        .build());
            }
        } catch (Exception e) {
            System.err.println("네임드 엘리먼트 추출 실패: " + e.getMessage());
        }
    }

    /**
     * 메시지에서 일본어 요소를 추출합니다.
     */
    private void extractFromMessage(IMessage message, List<DiagramNode> japaneseNodes) {
        try {
            // 메시지 이름
            String name = message.getName();
            if (name != null && !name.isEmpty() && JapaneseDetector.containsJapanese(name)) {
                japaneseNodes.add(DiagramNode.builder()
                        .id(message.getId() + "_name")
                        .name(name)
                        .type("sequence_message_name")
                        .build());
            }

            // 메시지 인자
            String argument = message.getArgument();
            if (argument != null && !argument.isEmpty() && JapaneseDetector.containsJapanese(argument)) {
                japaneseNodes.add(DiagramNode.builder()
                        .id(message.getId() + "_argument")
                        .name(argument)
                        .type("sequence_message_argument")
                        .build());
            }

            // 반환값
            String returnValue = message.getReturnValue();
            if (returnValue != null && !returnValue.isEmpty() && JapaneseDetector.containsJapanese(returnValue)) {
                japaneseNodes.add(DiagramNode.builder()
                        .id(message.getId() + "_return")
                        .name(returnValue)
                        .type("sequence_message_return")
                        .build());
            }

            // 가드 조건
            String guard = message.getGuard();
            if (guard != null && !guard.isEmpty() && JapaneseDetector.containsJapanese(guard)) {
                japaneseNodes.add(DiagramNode.builder()
                        .id(message.getId() + "_guard")
                        .name(guard)
                        .type("sequence_message_guard")
                        .build());
            }
        } catch (Exception e) {
            System.err.println("메시지 추출 실패: " + e.getMessage());
        }
    }

    /**
     * 메시지에 번역을 적용합니다.
     */
    private int applyTranslationsToMessage(IMessage message, Map<String, String> translationMap) {
        int count = 0;

        try {
            // 메시지 이름 번역
            String nameId = message.getId() + "_name";
            if (translationMap.containsKey(nameId)) {
                String originalName = message.getName();
                String translatedName = translationMap.get(nameId);
                message.setName(originalName + "\n" + translatedName);
                count++;
            }

            // 메시지 인자 번역
            String argumentId = message.getId() + "_argument";
            if (translationMap.containsKey(argumentId)) {
                String originalArgument = message.getArgument();
                String translatedArgument = translationMap.get(argumentId);
                message.setArgument(originalArgument + "\n" + translatedArgument);
                count++;
            }

            // 반환값 번역
            String returnId = message.getId() + "_return";
            if (translationMap.containsKey(returnId)) {
                String originalReturn = message.getReturnValue();
                String translatedReturn = translationMap.get(returnId);
                message.setReturnValue(originalReturn + "\n" + translatedReturn);
                count++;
            }

            // 가드 조건 번역
            String guardId = message.getId() + "_guard";
            if (translationMap.containsKey(guardId)) {
                String originalGuard = message.getGuard();
                String translatedGuard = translationMap.get(guardId);
                message.setGuard(originalGuard + "\n" + translatedGuard);
                count++;
            }
        } catch (Exception e) {
            System.err.println("메시지 번역 적용 실패: " + e.getMessage());
        }

        return count;
    }

    /**
     * 요소 타입을 문자열로 반환합니다.
     */
    private String getElementType(INamedElement element) {
        if (element instanceof IMessage) {
            return "sequence_message";
        } else if (element instanceof ILifeline) {
            return "sequence_lifeline";
        } else if (element instanceof IInteraction) {
            return "sequence_interaction";
        }
        return "sequence_unknown";
    }
}
