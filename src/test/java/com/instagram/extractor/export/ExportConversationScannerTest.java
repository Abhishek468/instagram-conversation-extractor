package com.instagram.extractor.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExportConversationScannerTest {

//     @Test
    void discoversConversationFilesAndMediaDirectories()
            throws Exception {

        Path exportRoot =
                Files.createTempDirectory(
                        "instagram-export-test"
                );

        Path conversationDirectory =
                exportRoot.resolve(
                        "your_instagram_activity")
                        .resolve("messages")
                        .resolve("inbox")
                        .resolve("test_conversation");

        Files.createDirectories(
                conversationDirectory
        );

        Files.writeString(
                conversationDirectory.resolve(
                        "message_1.json"),
                "{}"
        );

        Files.writeString(
                conversationDirectory.resolve(
                        "message_2.json"),
                "{}"
        );

        Files.writeString(
                conversationDirectory.resolve(
                        "message_3.json"),
                "{}"
        );

        // Should NOT be discovered as a message file.
        Files.writeString(
                conversationDirectory.resolve(
                        "unrelated.json"),
                "{}"
        );

        Path photosDirectory =
                conversationDirectory.resolve("photos");

        Path videosDirectory =
                conversationDirectory.resolve("videos");

        Files.createDirectories(photosDirectory);
        Files.createDirectories(videosDirectory);

        Files.writeString(
                photosDirectory.resolve("photo.jpg"),
                "test"
        );

        Files.writeString(
                videosDirectory.resolve("video.mp4"),
                "test"
        );

        ExportConversationScanner scanner =
                new ExportConversationScanner(
                        exportRoot
                );

        ExportConversationDescriptor result =
                scanner.scan(conversationDirectory);

        assertEquals(
                "test_conversation",
                result.conversationName()
        );

        assertEquals(
                conversationDirectory,
                result.conversationDirectory()
        );

        assertEquals(
                3,
                result.messageFiles().size()
        );

        assertTrue(
                result.messageFiles().stream()
                        .allMatch(path ->
                                path.getFileName()
                                        .toString()
                                        .matches(
                                                "message_\\d+\\.json"
                                        )
                        )
        );

        assertTrue(
                result.messageFiles().stream()
                        .noneMatch(path ->
                                path.getFileName()
                                        .toString()
                                        .equals(
                                                "unrelated.json"
                                        )
                        )
        );

        assertEquals(
                photosDirectory,
                result.photosDirectory()
        );

        assertEquals(
                videosDirectory,
                result.videosDirectory()
        );
    }
}