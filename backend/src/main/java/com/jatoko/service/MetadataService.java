package com.jatoko.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jatoko.model.TranslationMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class MetadataService {

    private final ObjectMapper objectMapper;

    public MetadataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 원본 파일에 대한 메타데이터 파일 경로를 반환합니다.
     * 예: test.asta -> test.asta.meta.json
     */
    public File getMetadataFile(File originalFile) {
        return new File(originalFile.getParentFile(), originalFile.getName() + ".meta.json");
    }

    /**
     * 메타데이터 파일을 로드합니다. 파일이 없거나 오류 발생 시 빈 메타데이터를 반환합니다.
     */
    public TranslationMetadata loadMetadata(File originalFile) {
        File metadataFile = getMetadataFile(originalFile);
        if (!metadataFile.exists()) {
            return new TranslationMetadata();
        }

        try {
            return objectMapper.readValue(metadataFile, TranslationMetadata.class);
        } catch (IOException e) {
            log.warn("메타데이터 로드 실패: {}", e.getMessage());
            return new TranslationMetadata();
        }
    }

    /**
     * 메타데이터를 파일에 저장합니다.
     */
    public void saveMetadata(File originalFile, TranslationMetadata metadata) {
        File metadataFile = getMetadataFile(originalFile);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile, metadata);
            log.info("메타데이터 저장 완료: {}", metadataFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("메타데이터 저장 실패: {}", e.getMessage(), e);
            throw new RuntimeException("메타데이터 저장 실패", e);
        }
    }

    /**
     * 파일의 SHA-256 해시를 계산합니다.
     */
    public String calculateHash(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("파일 해시 계산 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일 해시 계산 실패", e);
        }
    }
}
