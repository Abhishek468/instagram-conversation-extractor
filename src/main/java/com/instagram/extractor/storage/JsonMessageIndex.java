package com.instagram.extractor.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class JsonMessageIndex implements MessageIndex {

    private final Path indexFile;
    private final ObjectMapper objectMapper;
    private final Set<String> messageIds;

    public JsonMessageIndex(
            Path conversationDirectory,
            ObjectMapper objectMapper) throws IOException {

        Files.createDirectories(conversationDirectory);

        this.indexFile =
                conversationDirectory.resolve(
                        "message-index.json"
                );

        this.objectMapper =
                objectMapper.copy()
                        .enable(
                                SerializationFeature.INDENT_OUTPUT
                        );

        if (Files.exists(indexFile)) {

            this.messageIds =
                    objectMapper.readValue(
                            indexFile.toFile(),
                            new TypeReference<Set<String>>() {}
                    );

        } else {

            this.messageIds =
                    new HashSet<>();
        }
    }

    @Override
    public boolean contains(String messageId) {
        return messageIds.contains(messageId);
    }

    @Override
    public void add(String messageId) {

        if (messageId == null || messageId.isBlank()) {
            return;
        }

        messageIds.add(messageId);
    }

    @Override
    public int size() {
        return messageIds.size();
    }

    @Override
    public void save() throws IOException {

        objectMapper.writeValue(
                indexFile.toFile(),
                messageIds
        );
    }

    public Path getIndexFile() {
        return indexFile;
    }
}