package com.jatoko.controller;

import com.jatoko.service.SessionStorageService;
import com.jatoko.service.SvgParserService;
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

/**
 * SVG 파일 번역 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/svg")
@RequiredArgsConstructor
public class SvgController {

    private final SvgParserService svgParserService;
    private final SessionStorageService sessionStorage;
    private final FileUploadConfig fileUploadConfig;

    /**
     * SVG 파일 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadSvgFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다."));
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".svg")) {
                return ResponseEntity.badRequest().body(Map.of("error", "SVG 파일(.svg)만 업로드 가능합니다."));
            }

            String sessionId = UUID.randomUUID().toString();
            String filename = sessionId + "_" + originalFilename;
            Path uploadDir = Paths.get(fileUploadConfig.getUploadDir()).toAbsolutePath();
            Path filePath = uploadDir.resolve(filename);

            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            sessionStorage.saveFilePath(sessionId, filePath.toString());
            sessionStorage.saveOriginalFilename(sessionId, originalFilename);

            log.info("SVG 파일 업로드 완료: sessionId={}, file={}", sessionId, originalFilename);

            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "filename", originalFilename,
                "message", "SVG 파일이 성공적으로 업로드되었습니다."
            ));

        } catch (IOException e) {
            log.error("SVG 파일 업로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "파일 업로드 중 오류가 발생했습니다."));
        }
    }

    /**
     * 번역된 SVG 파일 다운로드
     */
    @GetMapping("/download/{sessionId}")
    public ResponseEntity<?> downloadTranslatedSvg(@PathVariable String sessionId) {
        try {
            String translatedKey = sessionId + "_translated";
            if (!sessionStorage.hasSession(translatedKey)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "번역된 파일을 찾을 수 없습니다. 먼저 번역을 적용해주세요."));
            }

            String filePath = sessionStorage.getFilePath(translatedKey);
            File file = new File(filePath);

            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "파일을 찾을 수 없습니다."));
            }

            String originalFilename = sessionStorage.getOriginalFilename(sessionId);
            String downloadFilename = "translated_" + originalFilename;

            // UTF-8 파일명 인코딩 (RFC 5987)
            String encodedFilename = java.net.URLEncoder.encode(downloadFilename, "UTF-8")
                .replaceAll("\\+", "%20");

            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .body(resource);

        } catch (Exception e) {
            log.error("SVG 파일 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "파일 다운로드 중 오류가 발생했습니다."));
        }
    }

    /**
     * 통합 번역 적용 (추출 → 번역 → 적용 일괄 처리)
     */
    @PostMapping("/apply-translation-integrated")
    public ResponseEntity<?> applyTranslationIntegrated(@RequestBody Map<String, String> request) {
        try {
            String sessionId = request.get("sessionId");
            if (sessionId == null || !sessionStorage.hasSession(sessionId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 세션 ID입니다."));
            }

            String filePath = sessionStorage.getFilePath(sessionId);
            File svgFile = new File(filePath);

            if (!svgFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "SVG 파일을 찾을 수 없습니다."));
            }

            // 출력 파일 경로 생성
            String originalFilename = sessionStorage.getOriginalFilename(sessionId);
            String translatedFilename = sessionId + "_translated_" + originalFilename;
            Path outputPath = Paths.get(fileUploadConfig.getUploadDir()).resolve(translatedFilename);
            File outputFile = outputPath.toFile();

            // 통합 처리: 추출 → 번역 → 적용
            svgParserService.extractTranslateAndApply(svgFile, outputFile);

            // 번역된 파일 경로 저장
            sessionStorage.saveFilePath(sessionId + "_translated", outputFile.getAbsolutePath());

            log.info("SVG 통합 번역 적용 완료: sessionId={}, output={}", sessionId, outputFile.getName());

            return ResponseEntity.ok(Map.of(
                "message", "통합 번역 적용 성공",
                "downloadUrl", "/api/svg/download/" + sessionId
            ));

        } catch (Exception e) {
            log.error("SVG 통합 번역 적용 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "통합 번역 적용 실패: " + e.getMessage()));
        }
    }
}
