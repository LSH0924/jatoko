package com.jatoko.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchTranslationRequest {
    private List<String> fileNames;
}
