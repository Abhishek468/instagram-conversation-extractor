package com.instagram.extractor.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record Message(
        String id,
        long timestampMs,
        Participant sender,
        String content,
        List<Reaction> reactions,
        String sharedLink,
        List<Attachment> attachments,
        JsonNode rawData
) {

    public Message {
        reactions =
                reactions == null
                        ? List.of()
                        : List.copyOf(reactions);

        attachments =
                attachments == null
                        ? List.of()
                        : List.copyOf(attachments);
    }

    /**
     * Backward-compatible constructor for the
     * existing live/archive code.
     */
    public Message(
            String id,
            long timestampMs,
            JsonNode rawData) {

        this(
                id,
                timestampMs,
                null,
                null,
                List.of(),
                null,
                List.of(),
                rawData
        );
    }
}