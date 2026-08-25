package com.instagram.extractor.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class ExportConversationScanner {

    private final Path exportRoot;

    public ExportConversationScanner(Path exportRoot) {
        this.exportRoot =
                Objects.requireNonNull(exportRoot, "exportRoot");
    }

    public ExportConversationDescriptor scan(
            Path conversationDirectory)
            throws IOException {

        Objects.requireNonNull(
                conversationDirectory,
                "conversationDirectory"
        );

        if (!Files.exists(exportRoot)) {
            throw new IllegalArgumentException(
                    "Export root does not exist: "
                            + exportRoot
            );
        }

        if (!Files.isDirectory(exportRoot)) {
            throw new IllegalArgumentException(
                    "Export root is not a directory: "
                            + exportRoot
            );
        }

        if (!Files.exists(conversationDirectory)) {
            throw new IllegalArgumentException(
                    "Conversation directory does not exist: "
                            + conversationDirectory
            );
        }

        if (!Files.isDirectory(conversationDirectory)) {
            throw new IllegalArgumentException(
                    "Conversation path is not a directory: "
                            + conversationDirectory
            );
        }

        List<Path> messageFiles;

        try (Stream<Path> files =
                     Files.list(conversationDirectory)) {

            messageFiles =
                    files
                            .filter(Files::isRegularFile)
                            .filter(this::isMessageFile)
                            .sorted(Comparator.comparing(
                                    Path::getFileName))
                            .toList();
        }

        Path photosDirectory =
                conversationDirectory.resolve("photos");

        Path videosDirectory =
                conversationDirectory.resolve("videos");

        return new ExportConversationDescriptor(
                conversationDirectory
                        .getFileName()
                        .toString(),
                conversationDirectory,
                messageFiles,
                Files.isDirectory(photosDirectory)
                        ? photosDirectory
                        : null,
                Files.isDirectory(videosDirectory)
                        ? videosDirectory
                        : null
        );
    }

    private boolean isMessageFile(Path path) {

        String fileName =
                path.getFileName()
                        .toString();

        return fileName.matches(
                "message_\\d+\\.json"
        );
    }
}