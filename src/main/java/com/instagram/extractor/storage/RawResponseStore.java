package com.instagram.extractor.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class RawResponseStore {

    private final Path syncDirectory;

    public RawResponseStore(
            Path outputDirectory,
            String conversationId) throws IOException {

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
    }

    public Path save(
            int pageNumber,
            String rawJson) throws IOException {

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

    public Path getSyncDirectory() {
        return syncDirectory;
    }
}