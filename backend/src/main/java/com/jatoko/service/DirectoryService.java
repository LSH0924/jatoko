package com.jatoko.service;

import com.jatoko.config.DirectoryConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;
import com.jatoko.dto.FileMetadataDto;
import com.jatoko.dto.BatchTranslationResponse;

import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.stream.Collectors;
import com.jatoko.util.SvgOutlineDetector;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final DirectoryConfig directoryConfig;
    private final AstahParserService astahParserService;
    private final SvgParserService svgParserService;
    private final ProgressService progressService;

    @PostConstruct
    public void init() {
        createDirectoryIfNotExists(directoryConfig.getTarget(), "Target");
        createDirectoryIfNotExists(directoryConfig.getTranslated(), "Translated");
    }

    private void createDirectoryIfNotExists(String path, String name) {
        File directory = new File(path);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.info("{} directory created at: {}", name, path);
            } else {
                log.error("Failed to create {} directory at: {}", name, path);
            }
        } else {
            log.info("{} directory already exists at: {}", name, path);
        }
    }

    public List<String> listFiles(String type) {
        String path;

        if ("target".equalsIgnoreCase(type)) {
            path = directoryConfig.getTarget();
        } else if ("translated".equalsIgnoreCase(type)) {
            path = directoryConfig.getTranslated();
        } else {
            throw new IllegalArgumentException("Invalid directory type: " + type);
        }

        File directory = new File(path);
        List<String> fileList = new ArrayList<>();
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory: " + type);
        }

        File[] files = directory.listFiles();
        if (files == null) {
            throw new RuntimeException("Directory is empty.");
        }

        for (File file : files) {
            boolean isShowable =!file.getName().startsWith(".");
            String lowerName = file.getName().toLowerCase();
            boolean hasTargetExtension =lowerName.endsWith(".asta") || lowerName.endsWith(".svg");
            if (file.isFile() && isShowable && hasTargetExtension) {
                fileList.add(file.getName());
            }
        }

        return fileList;
    }

    /**
     * 업로드 결과 DTO
     */
    public record UploadResult(String fileName, boolean outlined) {}

    public UploadResult uploadToTarget(MultipartFile file) throws IOException {
        String targetPath = directoryConfig.getTarget();
        String fileName = file.getOriginalFilename();
        Path destination = Paths.get(targetPath, fileName);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        log.info("File uploaded to target: {}", destination);

        // SVG 파일인 경우 아웃라인 여부 확인
        boolean outlined = false;
        if (fileName != null && fileName.toLowerCase().endsWith(".svg")) {
            outlined = SvgOutlineDetector.isOutlined(destination.toFile());
        }

        return new UploadResult(fileName, outlined);
    }

    public Resource downloadFromTranslated(String fileName) throws IOException {
        String translatedPath = directoryConfig.getTranslated();
        Path filePath = Paths.get(translatedPath, fileName);

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + fileName);
        }

        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("Cannot read file: " + fileName);
        }

        log.info("File downloaded from translated: {}", filePath);
        return resource;
    }

    /**
     * 파일명 중복을 방지하여 고유한 파일명 생성
     * 예: file_translated.asta가 존재하면 file_translated_1.asta, file_translated_2.asta ...
     */
    private String getUniqueFileName(String baseName, String extension) {
        String outputFileName = baseName + extension;
        Path outputPath = Paths.get(directoryConfig.getTranslated(), outputFileName);

        if (!Files.exists(outputPath)) {
            return outputFileName;
        }

        // 파일이 존재하면 _1, _2, _3... 형식으로 번호 추가
        int counter = 1;
        while (true) {
            outputFileName = baseName + "_" + counter + extension;
            outputPath = Paths.get(directoryConfig.getTranslated(), outputFileName);

            if (!Files.exists(outputPath)) {
                return outputFileName;
            }
            counter++;
        }
    }

    /**
     * target 디렉토리의 파일을 번역하여 translated 디렉토리에 저장 (진행률 보고 포함)
     */
    public String translateFile(String fileName) throws Exception {
        return translateFile(fileName, null);
    }

    public String translateFile(String fileName, String clientId) throws Exception {
        Path targetPath = Paths.get(directoryConfig.getTarget(), fileName);

        if (!Files.exists(targetPath)) {
            String error = "File not found: " + fileName;
            if (clientId != null) progressService.sendError(clientId, error);
            throw new IOException(error);
        }

        File inputFile = targetPath.toFile();
        String lowerFileName = fileName.toLowerCase();
        String outputFileName;
        Path outputPath;

        // 진행률 콜백 생성
        ProgressCallback callback = (message, percentage) -> {
            if (clientId != null) {
                progressService.sendProgress(clientId, message, percentage);
            }
        };

        try {
            if (lowerFileName.endsWith(".asta") || lowerFileName.endsWith(".astah")) {
                // Astah 파일 번역
                String baseName = fileName.replaceAll("\\.(asta|astah)$", "");
                outputFileName = getUniqueFileName(baseName + "_translated", ".asta");
                outputPath = Paths.get(directoryConfig.getTranslated(), outputFileName);

                astahParserService.extractTranslateAndApply(inputFile, outputPath.toFile(), callback);

            } else if (lowerFileName.endsWith(".svg")) {
                // SVG 파일 번역
                String baseName = fileName.replaceAll("\\.svg$", "");
                outputFileName = getUniqueFileName(baseName + "_translated", ".svg");
                outputPath = Paths.get(directoryConfig.getTranslated(), outputFileName);

                svgParserService.extractTranslateAndApply(inputFile, outputPath.toFile(), callback);

            } else {
                throw new IllegalArgumentException("Unsupported file type: " + fileName);
            }

            log.info("File translated: {} -> {}", fileName, outputFileName);
            if (clientId != null) {
                progressService.complete(clientId);
            }
            return outputFileName;

        } catch (Exception e) {
            if (clientId != null) {
                progressService.sendError(clientId, "Translation failed: " + e.getMessage());
            }
            throw e;
        }
    }

    /**
     * 지정된 디렉토리에서 파일 삭제
     * target 타입인 경우 meta.json과 번역된 파일들도 함께 삭제
     */
    public void deleteFile(String type, String fileName) throws IOException {
        String path;
        if ("target".equalsIgnoreCase(type)) {
            path = directoryConfig.getTarget();

            // 1. 원본 파일 삭제
            Path filePath = Paths.get(path, fileName);
            if (!Files.exists(filePath)) {
                throw new IOException("File not found: " + fileName);
            }
            Files.delete(filePath);
            log.info("File deleted from {}: {}", type, fileName);

            // 2. meta.json 파일 삭제
            Path metaFilePath = Paths.get(path, fileName + ".meta.json");
            if (Files.exists(metaFilePath)) {
                Files.delete(metaFilePath);
                log.info("Meta file deleted: {}", metaFilePath);
            }

            // 3. 대응하는 번역 파일들 삭제
            String baseName = fileName.replaceAll("\\.(asta|astah|svg)$", "");
            File translatedDir = new File(directoryConfig.getTranslated());
            if (translatedDir.exists() && translatedDir.isDirectory()) {
                List<File> translatedFiles = findTranslatedFiles(translatedDir, baseName);
                for (File translatedFile : translatedFiles) {
                    Files.delete(translatedFile.toPath());
                    log.info("Translated file deleted: {}", translatedFile.getName());
                }
            }

        } else if ("translated".equalsIgnoreCase(type)) {
            path = directoryConfig.getTranslated();

            Path filePath = Paths.get(path, fileName);
            if (!Files.exists(filePath)) {
                throw new IOException("File not found: " + fileName);
            }
            Files.delete(filePath);
            log.info("File deleted from {}: {}", type, fileName);

        } else {
            throw new IllegalArgumentException("Invalid directory type: " + type);
        }
    }

    /**
     * target 디렉토리의 파일 메타데이터 목록 조회
     */
    public List<FileMetadataDto> getFileMetadata() throws IOException {
        String targetPath = directoryConfig.getTarget();
        String translatedPath = directoryConfig.getTranslated();

        File targetDir = new File(targetPath);
        File translatedDir = new File(translatedPath);

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            throw new IllegalArgumentException("Target directory not found");
        }

        File[] targetFiles = targetDir.listFiles();
        if (targetFiles == null) {
            return new ArrayList<>();
        }

        List<FileMetadataDto> metadataList = new ArrayList<>();

        for (File targetFile : targetFiles) {
            String fileName = targetFile.getName();
            boolean isShowable = !fileName.startsWith(".");
            String lowerName = fileName.toLowerCase();
            boolean hasTargetExtension = lowerName.endsWith(".asta") || lowerName.endsWith(".svg");

            if (!targetFile.isFile() || !isShowable || !hasTargetExtension) {
                continue;
            }

            // 업로드 날짜 (target 파일의 최종 수정 날짜)
            LocalDateTime uploadedAt = getFileLastModified(targetFile.toPath());

            // 번역 여부 및 번역 날짜 확인
            boolean translated = false;
            LocalDateTime translatedAt = null;

            // 번역 버전 (번역 파일 개수)
            Integer version = null;

            if (translatedDir.exists() && translatedDir.isDirectory()) {
                // 번역된 파일 찾기 (fileName_translated.ext 또는 fileName_translated_N.ext)
                String baseName = fileName.replaceAll("\\.(asta|astah|svg)$", "");
                List<File> translatedFiles = findTranslatedFiles(translatedDir, baseName);

                if (!translatedFiles.isEmpty()) {
                    translated = true;
                    version = translatedFiles.size();
                    // 가장 최신 번역 파일의 날짜
                    translatedAt = translatedFiles.stream()
                            .map(f -> getFileLastModified(f.toPath()))
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                }
            }

            // SVG 파일인 경우 아웃라인 여부 확인
            boolean outlined = false;
            if (lowerName.endsWith(".svg")) {
                outlined = SvgOutlineDetector.isOutlined(targetFile);
            }

            FileMetadataDto metadata = FileMetadataDto.builder()
                    .fileName(fileName)
                    .translated(translated)
                    .uploadedAt(uploadedAt)
                    .translatedAt(translatedAt)
                    .outlined(outlined)
                    .version(version)
                    .build();

            metadataList.add(metadata);
        }

        return metadataList;
    }

    /**
     * 파일의 최종 수정 날짜를 LocalDateTime으로 반환
     */
    private LocalDateTime getFileLastModified(Path filePath) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            return LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            log.warn("Failed to read file attributes: {}", filePath, e);
            return null;
        }
    }

    /**
     * baseName에 해당하는 번역된 파일들을 찾음
     * 예: file → file_translated.asta, file_translated_1.asta, file_translated_2.asta
     */
    private List<File> findTranslatedFiles(File translatedDir, String baseName) {
        File[] files = translatedDir.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }

        String pattern = baseName + "_translated";

        return java.util.Arrays.stream(files)
                .filter(f -> {
                    String name = f.getName();
                    String nameWithoutExt = name.replaceAll("\\.(asta|astah|svg)$", "");
                    return nameWithoutExt.equals(pattern) || nameWithoutExt.startsWith(pattern + "_");
                })
                .collect(Collectors.toList());
    }

    /**
     * 여러 파일을 순차적으로 번역 (DeepL API 부하 방지)
     */
    public BatchTranslationResponse translateFilesInBatch(List<String> fileNames) {
        List<String> successFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        for (String fileName : fileNames) {
            try {
                log.info("Translating file: {}", fileName);
                String translatedFileName = translateFile(fileName);
                successFiles.add(fileName);
                log.info("Successfully translated: {} -> {}", fileName, translatedFileName);
            } catch (Exception e) {
                log.error("Failed to translate file: {}", fileName, e);
                failedFiles.add(fileName);
            }
        }

        return BatchTranslationResponse.builder()
                .successFiles(successFiles)
                .failedFiles(failedFiles)
                .totalCount(fileNames.size())
                .successCount(successFiles.size())
                .failedCount(failedFiles.size())
                .build();
    }

    /**
     * 여러 파일을 target 디렉토리에서 삭제
     */
    public void deleteFilesInBatch(List<String> fileNames) {
        for (String fileName : fileNames) {
            try {
                deleteFile("target", fileName);
            } catch (Exception e) {
                log.error("Failed to delete file: {}", fileName, e);
            }
        }
    }

    /**
     * target 파일명에 대응하는 최신 번역 파일 다운로드
     */
    public Resource downloadLatestTranslatedFile(String targetFileName) throws IOException {
        String translatedPath = directoryConfig.getTranslated();
        File translatedDir = new File(translatedPath);

        if (!translatedDir.exists() || !translatedDir.isDirectory()) {
            throw new IOException("Translated directory not found");
        }

        // 번역된 파일 찾기
        String baseName = targetFileName.replaceAll("\\.(asta|astah|svg)$", "");
        List<File> translatedFiles = findTranslatedFiles(translatedDir, baseName);

        if (translatedFiles.isEmpty()) {
            throw new IOException("No translated file found for: " + targetFileName);
        }

        // 가장 최신 파일 선택
        File latestFile = translatedFiles.stream()
                .max(Comparator.comparing(File::lastModified))
                .orElseThrow(() -> new IOException("Failed to find latest translated file"));

        Path filePath = latestFile.toPath();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("Cannot read file: " + latestFile.getName());
        }

        log.info("File prepared for download: {}", filePath);
        return resource;
    }
}
