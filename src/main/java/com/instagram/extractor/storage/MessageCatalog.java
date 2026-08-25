package com.instagram.extractor.storage;

import java.util.List;
import java.util.Map;

public interface MessageCatalog {

    void add(
            String messageId,
            long timestampMs
    );

    boolean contains(
            String messageId
    );

    Long getTimestamp(
            String messageId
    );

    int size();

    List<String> getMessageIdsNewestFirst();

    Map<String, Long> snapshot();

    void save() throws Exception;
}