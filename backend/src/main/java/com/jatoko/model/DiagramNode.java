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

package com.jatoko.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagramNode {
    private String id;
    private String name;
    private String type;
    private String translatedName;
    private String diagramId;  // 다이어그램 이름을 번역할 때 사용

    // 중복 단어 대응
    @Builder.Default
    private List<String> duplicateNodeIds = new ArrayList<>();  // 동일한 name을 가진 다른 노드들의 ID
    @Builder.Default
    private boolean isDuplicate = false;  // 이 노드가 중복 노드인지 (번역에서 제외할지)
}
