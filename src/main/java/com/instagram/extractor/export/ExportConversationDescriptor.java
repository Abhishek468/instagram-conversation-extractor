package com.instagram.extractor.export;

import java.nio.file.Path;
import java.util.List;

public record ExportConversationDescriptor(
        String conversationName,
        Path conversationDirectory,
        List<Path> messageFiles,
        Path photosDirectory,
        Path videosDirectory
) {
}