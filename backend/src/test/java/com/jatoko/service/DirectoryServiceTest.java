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

package com.jatoko.service;

import com.jatoko.config.DirectoryConfig;
import com.jatoko.dto.FileMetadataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DirectoryServiceTest {

    @Mock
    private DirectoryConfig directoryConfig;
    @Mock
    private AstahParserService astahParserService;
    @Mock
    private SvgParserService svgParserService;
    @Mock
    private ProgressService progressService;

    private DirectoryService directoryService;

    @TempDir
    Path tempDir;

    private Path targetDir;
    private Path translatedDir;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        
        targetDir = tempDir.resolve("target");
        translatedDir = tempDir.resolve("translated");
        Files.createDirectories(targetDir);
        Files.createDirectories(translatedDir);

        when(directoryConfig.getTarget()).thenReturn(targetDir.toString());
        when(directoryConfig.getTranslated()).thenReturn(translatedDir.toString());

        directoryService = new DirectoryService(directoryConfig, astahParserService, svgParserService, progressService);
    }

    @Test
    void testListFiles_FiltersExtensions() throws IOException {
        // Create various files in target
        Files.createFile(targetDir.resolve("file1.asta"));
        Files.createFile(targetDir.resolve("file2.svg"));
        Files.createFile(targetDir.resolve("file3.txt"));
        Files.createFile(targetDir.resolve(".hidden"));

        // Test
        List<String> files = directoryService.listFiles("target");

        // Verify
        assertEquals(2, files.size());
        assertTrue(files.contains("file1.asta"));
        assertTrue(files.contains("file2.svg"));
        assertFalse(files.contains("file3.txt"));
    }

    @Test
    void testUploadToTarget() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.asta", 
            "application/octet-stream", 
            "content".getBytes()
        );

        DirectoryService.UploadResult result = directoryService.uploadToTarget(file);

        assertEquals("test.asta", result.fileName());
        assertFalse(result.outlined());
        assertTrue(Files.exists(targetDir.resolve("test.asta")));
    }

    @Test
    void testTranslateFile_Astah() throws Exception {
        String fileName = "test.asta";
        Files.createFile(targetDir.resolve(fileName));
        
        String clientId = "test-client";

        // Execute
        String result = directoryService.translateFile(fileName, clientId);

        // Verify
        assertTrue(result.contains("test_translated"));
        assertTrue(result.endsWith(".asta"));
        
        // Verify parser called
        verify(astahParserService).extractTranslateAndApply(any(File.class), any(File.class), any(ProgressCallback.class));
        
        // Verify completion sent
        verify(progressService).complete(clientId);
    }

    @Test
    void testGetFileMetadata() throws IOException {
        // Setup files
        Files.createFile(targetDir.resolve("doc1.asta"));
        Files.createFile(targetDir.resolve("doc2.svg"));
        
        // Setup translated file for doc1
        Files.createFile(translatedDir.resolve("doc1_translated.asta"));

        // Execute
        List<FileMetadataDto> metadata = directoryService.getFileMetadata();

        // Verify
        assertEquals(2, metadata.size());
        
        FileMetadataDto doc1 = metadata.stream().filter(m -> m.getFileName().equals("doc1.asta")).findFirst().orElseThrow();
        assertTrue(doc1.isTranslated());
        assertEquals(1, doc1.getVersion());

        FileMetadataDto doc2 = metadata.stream().filter(m -> m.getFileName().equals("doc2.svg")).findFirst().orElseThrow();
        assertFalse(doc2.isTranslated());
    }
}
