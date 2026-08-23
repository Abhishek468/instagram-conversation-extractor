package com.instagram.extractor.storage;

import java.io.IOException;
import java.util.Optional;

public interface MessageIndex {

    boolean contains(String messageId);

    void add(
            String messageId,
            MessageLocation location
    );

    Optional<MessageLocation> getLocation(
            String messageId
    );

    int size();

    void save() throws IOException;
}