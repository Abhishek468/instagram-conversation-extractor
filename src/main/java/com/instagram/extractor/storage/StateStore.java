package com.instagram.extractor.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StateStore {

    private final Path stateFile;
    private final ObjectMapper objectMapper;

    public StateStore(
            Path conversationDirectory,
            ObjectMapper objectMapper) throws IOException {

        Files.createDirectories(conversationDirectory);

        this.stateFile =
                conversationDirectory.resolve("state.json");

        this.objectMapper =
                objectMapper.copy()
                        .enable(
                                SerializationFeature.INDENT_OUTPUT
                        );
    }

    public void save(
            ExtractionState state) throws IOException {

        objectMapper.writeValue(
                stateFile.toFile(),
                state
        );
    }

    public Path getStateFile() {
        return stateFile;
    }
}