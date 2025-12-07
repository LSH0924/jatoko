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
