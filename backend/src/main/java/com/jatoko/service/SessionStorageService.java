package com.jatoko.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 세션별 파일 및 번역 데이터를 저장하는 서비스
 */
@Service
public class SessionStorageService {

    // 세션별 파일 경로
    private final Map<String, String> sessionFiles = new HashMap<>();

    // 세션별 원본 파일명
    private final Map<String, String> sessionOriginalFilenames = new HashMap<>();

    // 세션별 번역 맵
    private final Map<String, Map<String, String>> sessionTranslations = new HashMap<>();

    public void saveFilePath(String sessionId, String filePath) {
        sessionFiles.put(sessionId, filePath);
    }

    public String getFilePath(String sessionId) {
        return sessionFiles.get(sessionId);
    }

    public boolean hasSession(String sessionId) {
        return sessionFiles.containsKey(sessionId);
    }

    public void saveOriginalFilename(String sessionId, String filename) {
        sessionOriginalFilenames.put(sessionId, filename);
    }

    public String getOriginalFilename(String sessionId) {
        return sessionOriginalFilenames.get(sessionId);
    }

}
