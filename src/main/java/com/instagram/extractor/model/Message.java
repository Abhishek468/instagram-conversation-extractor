package com.instagram.extractor.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record Message(
        String id,
        long timestampMs,
        Participant sender,
        String content,
        List<String> reactions,
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
     * Backward-compatible constructor for the existing
     * live Instagram extraction path.
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
                List.of(),
                rawData
        );
    }
}