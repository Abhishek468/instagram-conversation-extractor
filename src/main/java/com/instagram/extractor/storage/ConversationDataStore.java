package com.instagram.extractor.storage;

import com.instagram.extractor.model.Message;

import java.util.List;
import java.util.Optional;

public interface ConversationDataStore {

    List<Message> getMessages();

    List<Message> getMessages(
            int offset,
            int limit
    );

    Optional<Message> getMessage(
            String messageId
    );

    int size();
}