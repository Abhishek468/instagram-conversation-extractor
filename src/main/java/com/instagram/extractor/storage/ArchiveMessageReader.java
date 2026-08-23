package com.instagram.extractor.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagram.extractor.instagram.InstagramResponseParser;
import com.instagram.extractor.model.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArchiveMessageReader
        implements ConversationDataStore {

    private final Path conversationDirectory;
    private final ObjectMapper objectMapper;
    private final InstagramResponseParser parser;

    /*
     * Page cache.
     *
     * Key   = page file
     * Value = messages contained in that page
     *
     * LinkedHashMap allows us to implement a simple
     * bounded LRU cache later without changing the
     * public API.
     */
    private final Map<Path, List<Message>> pageCache =
            new LinkedHashMap<>();

    private final int maxCachedPages = 10;

    public ArchiveMessageReader(
            Path conversationDirectory,
            ObjectMapper objectMapper,
            InstagramResponseParser parser) {

        this.conversationDirectory =
                conversationDirectory;

        this.objectMapper =
                objectMapper;

        this.parser =
                parser;
    }

    @Override
    public List<Message> getMessages() {

        List<Message> result =
                new ArrayList<>();

        for (Path page : discoverPageFiles()) {

            result.addAll(
                    readPage(page)
            );
        }

        sortNewestFirst(result);

        return result;
    }

    @Override
    public List<Message> getMessages(
            int offset,
            int limit) {

        if (offset < 0) {
            throw new IllegalArgumentException(
                    "offset cannot be negative"
            );
        }

        if (limit <= 0) {
            return List.of();
        }

        List<Message> all =
                getMessages();

        if (offset >= all.size()) {
            return List.of();
        }

        int end =
                Math.min(
                        offset + limit,
                        all.size()
                );

        return List.copyOf(
                all.subList(offset, end)
        );
    }

    @Override
    public Optional<Message> getMessage(
            String messageId) {

        if (messageId == null ||
                messageId.isBlank()) {

            return Optional.empty();
        }

        for (Path page : discoverPageFiles()) {

            for (Message message :
                    readPage(page)) {

                if (messageId.equals(
                        message.id())) {

                    return Optional.of(message);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public int size() {

        int count = 0;

        for (Path page : discoverPageFiles()) {

            count += readPage(page).size();
        }

        return count;
    }

    private List<Path> discoverPageFiles() {

        if (!Files.exists(
                conversationDirectory)) {

            return List.of();
        }

        try {

            List<Path> pages =
                    new ArrayList<>();

            try (var syncDirectories =
                         Files.list(
                                 conversationDirectory)) {

                syncDirectories
                        .filter(Files::isDirectory)
                        .forEach(syncDirectory -> {

                            try {

                                try (var files =
                                             Files.list(
                                                     syncDirectory)) {

                                    files
                                            .filter(
                                                    this::isPageFile
                                            )
                                            .forEach(
                                                    pages::add
                                            );
                                }

                            } catch (IOException e) {

                                throw new RuntimeException(
                                        "Failed to read sync directory: "
                                                + syncDirectory,
                                        e
                                );
                            }
                        });
            }

            pages.sort(
                    Comparator.comparing(
                            Path::toString
                    )
            );

            return pages;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to discover archive pages",
                    e
            );
        }
    }

    private boolean isPageFile(
            Path path) {

        String name =
                path.getFileName()
                        .toString();

        return Files.isRegularFile(path)
                && name.startsWith("page_")
                && name.endsWith(".json");
    }

    private List<Message> readPage(
            Path page) {

        List<Message> cached =
                pageCache.get(page);

        if (cached != null) {
            return cached;
        }

        try {

            String rawJson =
                    Files.readString(page);

            InstagramResponseParser.ParsedResponse
                    parsed =
                    parser.parse(rawJson);

            List<Message> messages =
                    parsed.messages()
                            .stream()
                            .map(
                                    message ->
                                            new Message(
                                                    message.id(),
                                                    message.timestampMs(),
                                                    message.rawNode()
                                            )
                            )
                            .toList();

            cachePage(page, messages);

            return messages;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to read archive page: "
                            + page,
                    e
            );
        }
    }

    private void cachePage(
            Path page,
            List<Message> messages) {

        if (pageCache.size()
                >= maxCachedPages) {

            Path oldest =
                    pageCache.keySet()
                            .iterator()
                            .next();

            pageCache.remove(oldest);
        }

        pageCache.put(
                page,
                messages
        );
    }

    private void sortNewestFirst(
            List<Message> messages) {

        messages.sort(
                Comparator.comparingLong(
                        Message::timestampMs
                ).reversed()
        );
    }
}