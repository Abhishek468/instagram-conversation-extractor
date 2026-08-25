package com.instagram.extractor.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonMessageCatalog
        implements MessageCatalog {

    private final Path catalogFile;

    private final ObjectMapper objectMapper;

    private final Map<String, Long> timestamps =
            new HashMap<>();

    public JsonMessageCatalog(
            Path conversationDirectory,
            ObjectMapper objectMapper)
            throws Exception {

        this.catalogFile =
                conversationDirectory.resolve(
                        "message-catalog.json"
                );

        this.objectMapper =
                objectMapper;

        load();
    }

    private void load()
            throws Exception {

        if (!Files.exists(catalogFile)) {
            return;
        }

        Map<String, Long> loaded =
                objectMapper.readValue(
                        catalogFile.toFile(),
                        new TypeReference<Map<String, Long>>() {}
                );

        timestamps.clear();

        if (loaded != null) {
            timestamps.putAll(loaded);
        }
    }

    @Override
    public void add(
            String messageId,
            long timestampMs) {

        if (messageId == null
                || messageId.isBlank()) {

            return;
        }

        /*
         * Preserve the first known timestamp.
         *
         * A message should normally have the same
         * timestamp wherever it appears in overlapping
         * sync archives.
         */
        timestamps.putIfAbsent(
                messageId,
                timestampMs
        );
    }

    @Override
    public boolean contains(
            String messageId) {

        return timestamps.containsKey(
                messageId
        );
    }

    @Override
    public Long getTimestamp(
            String messageId) {

        return timestamps.get(
                messageId
        );
    }

    @Override
    public int size() {

        return timestamps.size();
    }

    @Override
    public List<String> getMessageIdsNewestFirst() {

        List<String> ids =
                new ArrayList<>(
                        timestamps.keySet()
                );

        ids.sort(
                Comparator
                        .comparingLong(
                                (String id) ->
                                        timestamps.get(id)
                        )
                        .reversed()
                        .thenComparing(
                                Comparator.naturalOrder()
                        )
        );

        return ids;
    }

    @Override
    public Map<String, Long> snapshot() {

        return new HashMap<>(
                timestamps
        );
    }

    @Override
    public void save()
            throws Exception {

        Files.createDirectories(
                catalogFile.getParent()
        );

        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(
                        catalogFile.toFile(),
                        timestamps
                );
    }
}