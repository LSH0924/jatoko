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

    @GetMapping("/files/{type}")
    public ResponseEntity<List<String>> listFiles(@PathVariable String type) {
        try {
            List<String> files = directoryService.listFiles(type);
            return ResponseEntity.ok(files);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/files/target")
    public ResponseEntity<?> uploadToTarget(@RequestParam("file") MultipartFile file) {
        try {
            var result = directoryService.uploadToTarget(file);
            return ResponseEntity.ok(Map.of(
                    "fileName", result.fileName(),
                    "outlined", result.outlined(),
                    "message", "Upload successful"
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/files/translated/{fileName}")
    public ResponseEntity<Resource> downloadFromTranslated(@PathVariable String fileName) {
        try {
            Resource resource = directoryService.downloadFromTranslated(fileName);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/translate-file")
    public ResponseEntity<?> translateTargetFile(@RequestBody Map<String, String> request) {
        try {
            String fileName = request.get("fileName");
            if (fileName == null || fileName.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "fileName is required"));
            }
            String translatedFileName = directoryService.translateFile(fileName);
            return ResponseEntity.ok(Map.of(
                    "sessionId", translatedFileName,
                    "message", "Translation successful"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Translation failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/files/{type}/{fileName}")
    public ResponseEntity<?> deleteFile(@PathVariable String type, @PathVariable String fileName) {
        try {
            directoryService.deleteFile(type, fileName);
            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/files/metadata")
    public ResponseEntity<?> getFileMetadata() {
        try {
            List<FileMetadataDto> metadata = directoryService.getFileMetadata();
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve file metadata: " + e.getMessage()));
        }
    }

    @PostMapping("/translate/batch")
    public ResponseEntity<?> translateBatch(@RequestBody BatchTranslationRequest request) {
        try {
            if (request.getFileNames() == null || request.getFileNames().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "fileNames is required"));
            }

            BatchTranslationResponse response = directoryService.translateFilesInBatch(request.getFileNames());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Batch translation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/files/batch-delete")
    public ResponseEntity<?> batchDelete(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> fileNames = request.get("fileNames");
            if (fileNames == null || fileNames.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "fileNames is required"));
            }

            directoryService.deleteFilesInBatch(fileNames);
            return ResponseEntity.ok(Map.of("message", "Files deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Batch delete failed: " + e.getMessage()));
        }
    }

    @GetMapping("/download/translated/{targetFileName}")
    public ResponseEntity<Resource> downloadLatestTranslated(@PathVariable String targetFileName) {
        try {
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
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
