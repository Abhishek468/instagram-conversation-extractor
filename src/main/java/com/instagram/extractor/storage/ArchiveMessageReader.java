package com.instagram.extractor.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagram.extractor.instagram.InstagramResponseParser;
import com.instagram.extractor.model.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArchiveMessageReader
        implements ConversationDataStore {

    private final Path conversationDirectory;

    private final ObjectMapper objectMapper;

    private final InstagramResponseParser parser;

    private final MessageIndex messageIndex;

    /*
     * Page cache.
     *
     * Key   = page path
     * Value = messages contained in that page
     *
     * This prevents repeatedly parsing the same page
     * when several messages from the same page are requested.
     */
    private final Map<Path, List<Message>> pageCache =
            new LinkedHashMap<>();

    private final int maxCachedPages = 10;

    public ArchiveMessageReader(
            Path conversationDirectory,
            ObjectMapper objectMapper,
            InstagramResponseParser parser,
            MessageIndex messageIndex) {

        this.conversationDirectory =
                conversationDirectory;

        this.objectMapper =
                objectMapper;

        this.parser =
                parser;

        this.messageIndex =
                messageIndex;
    }

    @Override
    public List<Message> getMessages() {

        List<Message> result =
                new ArrayList<>();

        /*
         * We still discover all pages for the full conversation
         * operation.
         *
         * This will be optimized separately when we build
         * proper conversation pagination.
         */
        for (Path page : discoverPageFiles()) {

            result.addAll(
                    readPage(page)
            );
        }

        /*
         * Multiple sync archives can contain the same message.
         *
         * Deduplicate by message ID before returning the
         * logical conversation.
         */
        Map<String, Message> uniqueMessages =
                new LinkedHashMap<>();

        for (Message message : result) {

            uniqueMessages.putIfAbsent(
                    message.id(),
                    message
            );
        }

        List<Message> logicalMessages =
                new ArrayList<>(
                        uniqueMessages.values()
                );

        sortNewestFirst(
                logicalMessages
        );

        return logicalMessages;
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
                all.subList(
                        offset,
                        end
                )
        );
    }

    @Override
    public Optional<Message> getMessage(
            String messageId) {

        if (messageId == null
                || messageId.isBlank()) {

            return Optional.empty();
        }

        /*
         * ========================================================
         * INDEXED LOOKUP
         * ========================================================
         *
         * Instead of scanning every archive page:
         *
         *     message ID
         *          ↓
         *     index lookup
         *          ↓
         *     exact sync/page
         *          ↓
         *     read ONE page
         */
        Optional<MessageLocation> locationOptional =
                messageIndex.getLocation(
                        messageId
                );

        if (locationOptional.isEmpty()) {

            return Optional.empty();
        }

        MessageLocation location =
                locationOptional.get();

        Path page =
                conversationDirectory
                        .resolve(
                                location.syncId()
                        )
                        .resolve(
                                location.page()
                        );

        if (!Files.exists(page)) {

            throw new IllegalStateException(
                    "Indexed page does not exist: "
                            + page
            );
        }

        for (Message message :
                readPage(page)) {

            if (messageId.equals(
                    message.id()
            )) {

                return Optional.of(
                        message
                );
            }
        }

        /*
         * The index says the message exists in this page,
         * but the message could not be found there.
         *
         * This indicates archive/index inconsistency.
         */
        throw new IllegalStateException(
                "Message indexed at "
                        + page
                        + " but message ID was not found"
        );
    }

    @Override
    public int size() {

        return messageIndex.size();
    }

    private List<Path> discoverPageFiles() {

        if (!Files.exists(
                conversationDirectory
        )) {

            return List.of();
        }

        try {

            List<Path> pages =
                    new ArrayList<>();

            try (var syncDirectories =
                         Files.list(
                                 conversationDirectory
                         )) {

                syncDirectories
                        .filter(Files::isDirectory)
                        .forEach(
                                syncDirectory -> {

                                    try {

                                        try (var files =
                                                     Files.list(
                                                             syncDirectory
                                                     )) {

                                            files
                                                    .filter(
                                                            this::isPageFile
                                                    )
                                                    .forEach(
                                                            pages::add
                                                    );
                                        }

                                    } catch (
                                            IOException e
                                    ) {

                                        throw new RuntimeException(
                                                "Failed to read sync directory: "
                                                        + syncDirectory,
                                                e
                                        );
                                    }
                                }
                        );
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
                    Files.readString(
                            page
                    );

            InstagramResponseParser
                    .ParsedResponse parsed =
                    parser.parse(
                            rawJson
                    );

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

            cachePage(
                    page,
                    messages
            );

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

            pageCache.remove(
                    oldest
            );
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