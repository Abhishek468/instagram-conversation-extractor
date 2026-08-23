package com.instagram.extractor.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class RawResponseStore {

    private final Path syncDirectory;

    private final Path manifestFile;

    private final ObjectMapper objectMapper;

    public RawResponseStore(
            Path outputDirectory,
            String conversationId,
            ObjectMapper objectMapper)
            throws IOException {

        this.objectMapper =
                objectMapper.copy()
                        .enable(
                                SerializationFeature.INDENT_OUTPUT
                        );

        String syncId =
                "sync_" +
                java.time.LocalDateTime.now()
                        .toString()
                        .replace(":", "")
                        .replace(".", "");

        this.syncDirectory =
                outputDirectory
                        .resolve(conversationId)
                        .resolve(syncId);

        Files.createDirectories(
                syncDirectory
        );

        this.manifestFile =
                syncDirectory.resolve(
                        "manifest.json"
                );
    }

    public Path save(
            int pageNumber,
            String rawJson)
            throws IOException {

        String fileName =
                String.format(
                        "page_%06d.json",
                        pageNumber
                );

        Path file =
                syncDirectory.resolve(
                        fileName
                );

        Files.writeString(
                file,
                rawJson,
                StandardCharsets.UTF_8
        );

        return file;
    }

    public void saveManifest(
            ExtractionManifest manifest)
            throws IOException {

        objectMapper.writeValue(
                manifestFile.toFile(),
                manifest
        );
    }

    public Path getSyncDirectory() {
        return syncDirectory;
    }
    
    public Path getConversationDirectory() {
    return syncDirectory.getParent();
}

    public Path getManifestFile() {
        return manifestFile;
    }
}