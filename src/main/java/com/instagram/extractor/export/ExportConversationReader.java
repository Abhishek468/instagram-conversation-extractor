package com.instagram.extractor.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagram.extractor.model.Conversation;
import com.instagram.extractor.model.Message;
import com.instagram.extractor.model.Participant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ExportConversationReader {

    private final ObjectMapper objectMapper;
    private final ExportMessageParser messageParser;

    public ExportConversationReader(
            ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
        this.messageParser =
                new ExportMessageParser(objectMapper);
    }

    public Conversation read(
            Path conversationDirectory)
            throws IOException {

        if (conversationDirectory == null) {
            throw new IllegalArgumentException(
                    "conversationDirectory must not be null"
            );
        }

        if (!Files.isDirectory(conversationDirectory)) {
            throw new IllegalArgumentException(
                    "Conversation directory does not exist: "
                            + conversationDirectory
            );
        }

        List<Path> messageFiles =
                findMessageFiles(
                        conversationDirectory
                );

        if (messageFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "No message_N.json files found in: "
                            + conversationDirectory
            );
        }

        List<Message> allMessages =
                new ArrayList<>();

        Map<String, Participant> participants =
                new LinkedHashMap<>();

        for (Path messageFile : messageFiles) {

            JsonNode root =
                    objectMapper.readTree(
                            messageFile.toFile()
                    );

            collectParticipants(
                    root,
                    participants
            );

            List<Message> messages =
                    messageParser.parse(
                            root,
                            messageFile.getFileName()
                                    .toString()
                    );

            allMessages.addAll(messages);
        }

        allMessages.sort(
                Comparator.comparingLong(
                        Message::timestampMs
                )
        );

        String conversationId =
                determineConversationId(
                        messageFiles
                );

        return new Conversation(
                conversationId,
                new ArrayList<>(
                        participants.values()
                ),
                allMessages
        );
    }

    private List<Path> findMessageFiles(
            Path conversationDirectory)
            throws IOException {

        try (Stream<Path> files =
                     Files.list(conversationDirectory)) {

            return files
                    .filter(Files::isRegularFile)
                    .filter(this::isMessageFile)
                    .sorted(
                            Comparator.comparing(
                                    path ->
                                            path.getFileName()
                                                    .toString()
                            )
                    )
                    .toList();
        }
    }

    private boolean isMessageFile(Path path) {

        String fileName =
                path.getFileName()
                        .toString();

        return fileName.matches(
                "message_[0-9]+\\.json"
        );
    }

    private void collectParticipants(
            JsonNode root,
            Map<String, Participant> participants) {

        JsonNode participantsNode =
                root.path("participants");

        if (!participantsNode.isArray()) {
            return;
        }

        for (JsonNode participantNode :
                participantsNode) {

            String name =
                    participantNode.path("name")
                            .asText(null);

            if (name != null
                    && !name.isBlank()) {

                participants.putIfAbsent(
                        name,
                        new Participant(name)
                );
            }
        }
    }

    private String determineConversationId(
            List<Path> messageFiles) {

        if (messageFiles.isEmpty()) {
            return "export:unknown";
        }

        Path conversationDirectory =
                messageFiles.get(0)
                        .getParent();

        Path fileName =
                conversationDirectory
                        .getFileName();

        return fileName == null
                ? "export:unknown"
                : "export:"
                + fileName;
    }
}