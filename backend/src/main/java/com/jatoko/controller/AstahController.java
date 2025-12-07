package com.jatoko.controller;

import com.jatoko.config.FileUploadConfig;
import com.jatoko.service.AstahParserService;
import com.jatoko.service.SessionStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;


@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AstahController {

    private final AstahParserService astahParserService;
    private final FileUploadConfig fileUploadConfig;
    private final SessionStorageService sessionStorage;

    /**
     * Astah 파일 업로드
     */
    @PostMapping("/upload/astah")
    public ResponseEntity<?> uploadAstahFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다."));
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || (!originalFilename.endsWith(".asta") && !originalFilename.endsWith(".astah"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Astah 파일(.asta, .astah)만 업로드 가능합니다."));
            }

            String sessionId = UUID.randomUUID().toString();
            String filename = sessionId + "_" + originalFilename;
            Path uploadDir = Paths.get(fileUploadConfig.getUploadDir()).toAbsolutePath();
            Path filePath = uploadDir.resolve(filename);

            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            sessionStorage.saveFilePath(sessionId, filePath.toString());
            sessionStorage.saveOriginalFilename(sessionId, originalFilename);

            log.info("Astah 파일 업로드 완료: {}", filename);

            return ResponseEntity.ok(Map.of(
                    "sessionId", sessionId,
                    "filename", originalFilename,
                    "size", file.getSize(),
                    "message", "파일 업로드 성공"
            ));
        } catch (IOException e) {
            log.error("파일 업로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "파일 업로드 실패: " + e.getMessage()));
        }
    }

    /**
     * 수정된 Astah 파일 다운로드
     */
    @GetMapping("/download/{sessionId}")
    public ResponseEntity<Resource> downloadTranslatedFile(@PathVariable String sessionId) {
        try {
            String outputFilename = sessionId + "_translated.asta";
            File file = new File(fileUploadConfig.getUploadDir(), outputFilename);

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 원본 파일명 가져오기
            String originalFilename = sessionStorage.getOriginalFilename(sessionId);
            if (originalFilename == null) {
                originalFilename = "translated.asta";
            }
            
            // 확장자 제거
            String baseName = originalFilename.replaceAll("\\.(asta|astah)$", "");
            // _translated 추가
            String downloadFilename = baseName + "_translated.asta";

            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            log.error("파일 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 통합 번역 적용: 추출 → 번역 → 적용을 한 세션에서 메모리에서 직접 처리
     * ProjectAccessor를 닫지 않고 유지하여 Frame, Partition, Note 등의 번역 적용 보장
     */
    @PostMapping("/apply-translation-integrated")
    public ResponseEntity<?> applyTranslationIntegrated(@RequestBody Map<String, String> request) {
        try {
            String sessionId = request.get("sessionId");
            if (sessionId == null || !sessionStorage.hasSession(sessionId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 세션 ID입니다."));
            }

            String astahFilePath = sessionStorage.getFilePath(sessionId);
            File astahFile = new File(astahFilePath);

            if (!astahFile.exists()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Astah 파일을 찾을 수 없습니다."));
            }

            String outputFilename = sessionId + "_translated.asta";
            File outputFile = new File(fileUploadConfig.getUploadDir(), outputFilename);

            // 통합 처리: ProjectAccessor를 한 번만 열고 추출 → 번역 → 적용
            astahParserService.extractTranslateAndApply(astahFile, outputFile);

            log.info("통합 번역 적용 완료: {}", outputFilename);

            return ResponseEntity.ok(Map.of(
                    "message", "통합 번역 적용 성공",
                    "downloadUrl", "/api/download/" + sessionId
            ));
        } catch (Exception e) {
            log.error("통합 번역 적용 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "통합 번역 적용 실패: " + e.getMessage()));
        }
    }
}
