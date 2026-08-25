package com.instagram.extractor.model;

import java.util.List;

public record Conversation(
        String id,
        List<Participant> participants,
        List<Message> messages
) {

    public Conversation {
        participants =
                participants == null
                        ? List.of()
                        : List.copyOf(participants);

        messages =
                messages == null
                        ? List.of()
                        : List.copyOf(messages);
    }
}