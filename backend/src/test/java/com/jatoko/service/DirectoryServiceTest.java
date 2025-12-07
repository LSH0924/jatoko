package com.jatoko.service;

import com.jatoko.config.DirectoryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DirectoryServiceTest {

    @Mock
    private DirectoryConfig directoryConfig;
    @Mock
    private AstahParserService astahParserService;
    @Mock
    private SvgParserService svgParserService;

    private DirectoryService directoryService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        directoryService = new DirectoryService(directoryConfig, astahParserService, svgParserService);
    }

    @Test
    void testListFiles_Translated_FiltersExtensions() throws IOException {
        // Setup translated directory
        Path translatedDir = tempDir.resolve("translated");
        Files.createDirectories(translatedDir);
        
        // Create various files
        Files.createFile(translatedDir.resolve("file1.asta"));
        Files.createFile(translatedDir.resolve("file2.svg"));
        Files.createFile(translatedDir.resolve("file3.txt"));
        Files.createFile(translatedDir.resolve("file4.meta.json"));
        Files.createFile(translatedDir.resolve(".hidden"));

        when(directoryConfig.getTranslated()).thenReturn(translatedDir.toString());

        // Test
        List<String> files = directoryService.listFiles("translated");

        // Verify
        assertEquals(2, files.size());
        assertTrue(files.contains("file1.asta"));
        assertTrue(files.contains("file2.svg"));
        assertFalse(files.contains("file3.txt"));
        assertFalse(files.contains("file4.meta.json"));
        assertFalse(files.contains(".hidden"));
    }

    @Test
    void testListFiles_Target_DoesNotFilterExtensions() throws IOException {
        // Setup target directory
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        
        // Create various files
        Files.createFile(targetDir.resolve("file1.asta"));
        Files.createFile(targetDir.resolve("file3.txt"));

        when(directoryConfig.getTarget()).thenReturn(targetDir.toString());

        // Test
        List<String> files = directoryService.listFiles("target");

        // Verify
        assertEquals(1, files.size());
        assertTrue(files.contains("file1.asta"));
    }
}
