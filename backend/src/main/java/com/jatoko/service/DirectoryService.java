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

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final DirectoryConfig directoryConfig;
    private final AstahParserService astahParserService;
    private final SvgParserService svgParserService;

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

    public String uploadToTarget(MultipartFile file) throws IOException {
        String targetPath = directoryConfig.getTarget();
        Path destination = Paths.get(targetPath, file.getOriginalFilename());
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        log.info("File uploaded to target: {}", destination);
        return file.getOriginalFilename();
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
     * target 디렉토리의 파일을 번역하여 translated 디렉토리에 저장
     */
    public String translateFile(String fileName) throws Exception {
        Path targetPath = Paths.get(directoryConfig.getTarget(), fileName);

        if (!Files.exists(targetPath)) {
            throw new IOException("File not found: " + fileName);
        }

        File inputFile = targetPath.toFile();
        String lowerFileName = fileName.toLowerCase();
        String outputFileName;
        Path outputPath;

        if (lowerFileName.endsWith(".asta") || lowerFileName.endsWith(".astah")) {
            // Astah 파일 번역
            String baseName = fileName.replaceAll("\\.(asta|astah)$", "");
            outputFileName = getUniqueFileName(baseName + "_translated", ".asta");
            outputPath = Paths.get(directoryConfig.getTranslated(), outputFileName);

            astahParserService.extractTranslateAndApply(inputFile, outputPath.toFile());

        } else if (lowerFileName.endsWith(".svg")) {
            // SVG 파일 번역
            String baseName = fileName.replaceAll("\\.svg$", "");
            outputFileName = getUniqueFileName(baseName + "_translated", ".svg");
            outputPath = Paths.get(directoryConfig.getTranslated(), outputFileName);

            svgParserService.extractTranslateAndApply(inputFile, outputPath.toFile());

        } else {
            throw new IllegalArgumentException("Unsupported file type: " + fileName);
        }

        log.info("File translated: {} -> {}", fileName, outputFileName);
        return outputFileName;
    }

    /**
     * 지정된 디렉토리에서 파일 삭제
     */
    public void deleteFile(String type, String fileName) throws IOException {
        String path;
        if ("target".equalsIgnoreCase(type)) {
            path = directoryConfig.getTarget();
        } else if ("translated".equalsIgnoreCase(type)) {
            path = directoryConfig.getTranslated();
        } else {
            throw new IllegalArgumentException("Invalid directory type: " + type);
        }

        Path filePath = Paths.get(path, fileName);

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + fileName);
        }

        Files.delete(filePath);
        log.info("File deleted from {}: {}", type, fileName);
    }
}
