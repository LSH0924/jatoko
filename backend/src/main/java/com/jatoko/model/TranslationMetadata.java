package com.jatoko.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationMetadata {
    private String originalFileHash;
    private long lastModified;
    
    @Builder.Default
    private Map<String, NodeTranslation> translations = new HashMap<>();
}
