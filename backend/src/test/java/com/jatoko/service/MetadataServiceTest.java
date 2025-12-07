package com.jatoko.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jatoko.model.NodeTranslation;
import com.jatoko.model.TranslationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MetadataServiceTest {

    private MetadataService metadataService;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        metadataService = new MetadataService(objectMapper);
    }

    @Test
    void testGetMetadataFile() {
        File originalFile = new File(tempDir.toFile(), "test.asta");
        File metadataFile = metadataService.getMetadataFile(originalFile);

        assertEquals(new File(tempDir.toFile(), "test.asta.meta.json"), metadataFile);
    }

    @Test
    void testSaveAndLoadMetadata() {
        File originalFile = new File(tempDir.toFile(), "test.asta");
        TranslationMetadata metadata = new TranslationMetadata();
        metadata.setOriginalFileHash("hash123");
        metadata.setLastModified(123456789L);
        metadata.setTranslations(Map.of(
            "id1", new NodeTranslation("id1", "original", "translated")
        ));

        metadataService.saveMetadata(originalFile, metadata);

        TranslationMetadata loadedMetadata = metadataService.loadMetadata(originalFile);

        assertEquals("hash123", loadedMetadata.getOriginalFileHash());
        assertEquals(123456789L, loadedMetadata.getLastModified());
        assertEquals(1, loadedMetadata.getTranslations().size());
        assertEquals("translated", loadedMetadata.getTranslations().get("id1").getTranslatedText());
    }

    @Test
    void testLoadMetadata_FileNotFound() {
        File originalFile = new File(tempDir.toFile(), "nonexistent.asta");
        TranslationMetadata metadata = metadataService.loadMetadata(originalFile);

        assertNotNull(metadata);
        assertNull(metadata.getOriginalFileHash());
        assertTrue(metadata.getTranslations().isEmpty());
    }

    @Test
    void testCalculateHash() throws IOException {
        File file = new File(tempDir.toFile(), "test.txt");
        Files.writeString(file.toPath(), "test content");

        String hash = metadataService.calculateHash(file);

        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        
        // Same content should produce same hash
        File file2 = new File(tempDir.toFile(), "test2.txt");
        Files.writeString(file2.toPath(), "test content");
        String hash2 = metadataService.calculateHash(file2);
        
        assertEquals(hash, hash2);
        
        // Different content should produce different hash
        Files.writeString(file2.toPath(), "different content");
        String hash3 = metadataService.calculateHash(file2);
        
        assertNotEquals(hash, hash3);
    }
}
