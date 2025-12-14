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

package com.jatoko.controller;

import com.jatoko.dto.BatchTranslationRequest;
import com.jatoko.dto.BatchTranslationResponse;
import com.jatoko.dto.FileMetadataDto;
import com.jatoko.service.DirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;
    private final com.jatoko.service.ProgressService progressService;

    @GetMapping(value = "/progress/subscribe/{clientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter subscribe(@PathVariable String clientId) {
        return progressService.createEmitter(clientId);
    }

    @GetMapping("/files/{type}")
    public ResponseEntity<List<String>> listFiles(@PathVariable String type) {
        List<String> files = directoryService.listFiles(type);
        return ResponseEntity.ok(files);
    }

    @PostMapping("/files/target")
    public ResponseEntity<?> uploadToTarget(@RequestParam("file") MultipartFile file) throws IOException {
        var result = directoryService.uploadToTarget(file);
        return ResponseEntity.ok(Map.of(
                "fileName", result.fileName(),
                "outlined", result.outlined(),
                "message", "Upload successful"
        ));
    }

    @GetMapping("/files/translated/{fileName}")
    public ResponseEntity<Resource> downloadFromTranslated(@PathVariable String fileName) throws IOException {
        Resource resource = directoryService.downloadFromTranslated(fileName);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    @PostMapping("/translate-file")
    public ResponseEntity<?> translateTargetFile(@RequestBody Map<String, String> request) {
        String fileName = request.get("fileName");
        String clientId = request.get("clientId"); // Optional client ID for SSE

        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("fileName is required");
        }
        
        // Exception is handled by GlobalExceptionHandler, but we might want to return 
        // the result. The service will handle SSE updates internally.
        // However, DirectoryService.translateFile throws Exception, so we need to catch/rethrow or let Global handle it.
        // GlobalExceptionHandler handles general Exceptions.
        
        try {
            String translatedFileName = directoryService.translateFile(fileName, clientId);
            return ResponseEntity.ok(Map.of(
                    "sessionId", translatedFileName,
                    "message", "Translation successful"
            ));
        } catch (Exception e) {
            // If SSE is used, the service already sent an error event.
            // But we still return an error response for the HTTP request.
            throw new RuntimeException(e);
        }
    }

    @DeleteMapping("/files/{type}/{fileName}")
    public ResponseEntity<?> deleteFile(@PathVariable String type, @PathVariable String fileName) throws IOException {
        directoryService.deleteFile(type, fileName);
        return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
    }

    @GetMapping("/files/metadata")
    public ResponseEntity<?> getFileMetadata() throws IOException {
        List<FileMetadataDto> metadata = directoryService.getFileMetadata();
        return ResponseEntity.ok(metadata);
    }

    @PostMapping("/translate/batch")
    public ResponseEntity<?> translateBatch(@RequestBody BatchTranslationRequest request) {
        if (request.getFileNames() == null || request.getFileNames().isEmpty()) {
            throw new IllegalArgumentException("fileNames is required");
        }

        BatchTranslationResponse response = directoryService.translateFilesInBatch(request.getFileNames());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/files/batch-delete")
    public ResponseEntity<?> batchDelete(@RequestBody Map<String, List<String>> request) {
        List<String> fileNames = request.get("fileNames");
        if (fileNames == null || fileNames.isEmpty()) {
            throw new IllegalArgumentException("fileNames is required");
        }

        directoryService.deleteFilesInBatch(fileNames);
        return ResponseEntity.ok(Map.of("message", "Files deleted successfully"));
    }

    @GetMapping("/download/translated/{targetFileName}")
    public ResponseEntity<Resource> downloadLatestTranslated(@PathVariable String targetFileName) throws IOException {
        Resource resource = directoryService.downloadLatestTranslatedFile(targetFileName);

        // 실제 번역 파일명 추출
        String actualFileName = resource.getFilename();
        if (actualFileName == null) {
            actualFileName = targetFileName;
        }

        String encodedFileName = URLEncoder.encode(actualFileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }
}
