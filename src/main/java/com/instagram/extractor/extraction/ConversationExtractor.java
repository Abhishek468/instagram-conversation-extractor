package com.instagram.extractor.extraction;

import com.instagram.extractor.config.InstagramConfig;
import com.instagram.extractor.instagram.InstagramClient;
import com.instagram.extractor.instagram.InstagramResponseParser;
import com.instagram.extractor.storage.ExtractionManifest;
import com.instagram.extractor.storage.ExtractionState;
import com.instagram.extractor.storage.MessageIndex;
import com.instagram.extractor.storage.MessageLocation;
import com.instagram.extractor.storage.RawResponseStore;
import com.instagram.extractor.storage.StateStore;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class ConversationExtractor {

    private final InstagramConfig config;
    private final InstagramClient client;
    private final InstagramResponseParser parser;
    private final RawResponseStore store;
    private final StateStore stateStore;
    private final MessageIndex messageIndex;

    public ConversationExtractor(
            InstagramConfig config,
            InstagramClient client,
            InstagramResponseParser parser,
            RawResponseStore store,
            StateStore stateStore,
            MessageIndex messageIndex) {

        this.config = config;
        this.client = client;
        this.parser = parser;
        this.store = store;
        this.stateStore = stateStore;
        this.messageIndex = messageIndex;
    }

    public void extract() throws Exception {

        String newestMessageId = null;
        long newestTimestampMs = 0;

        String oldestMessageId = null;
        long oldestTimestampMs = Long.MAX_VALUE;

        int totalMessages = 0;

        OffsetDateTime startedAt =
                OffsetDateTime.now(
                        ZoneOffset.systemDefault()
                );

        ExtractionManifest manifest =
                new ExtractionManifest(
                        config.conversationId(),
                        startedAt,
                        config.first()
                );

        store.saveManifest(manifest);

        String afterCursor = null;

        int page = 1;

        String stopReason =
                "UNKNOWN";

        System.out.println();
        System.out.println(
                "========================================"
        );
        System.out.println(
                "STARTING INSTAGRAM EXTRACTION"
        );
        System.out.println(
                "Conversation: " +
                config.conversationId()
        );
        System.out.println(
                "Max pages: " +
                config.maxPages()
        );
        System.out.println(
                "========================================"
        );

        while (page <= config.maxPages()) {

            System.out.println();
            System.out.println(
                    "REQUESTING PAGE " + page
            );

            String rawJson =
                    client.fetchPage(
                            afterCursor
                    );

            Path savedFile =
                    store.save(
                            page,
                            rawJson
                    );

            InstagramResponseParser.ParsedResponse
                    parsed =
                    parser.parse(rawJson);

            totalMessages += parsed.nodeCount();

            /*
             * ====================================================
             * MESSAGE INDEX
             * ====================================================
             *
             * The raw response has already been persisted above.
             *
             * Now extract the message IDs and add them to the
             * persistent message index.
             *
             * The index contains only message IDs, not the
             * complete message payloads.
             */
           String syncId =
        savedFile
                .getParent()
                .getFileName()
                .toString();

String pageFileName =
        savedFile
                .getFileName()
                .toString();

MessageLocation location =
        new MessageLocation(
                syncId,
                pageFileName
        );

for (String messageId :
        parsed.messageIds()) {

    messageIndex.add(
            messageId,
            location
    );
}

            /*
             * Persist the index after every successfully processed
             * page.
             *
             * This means that if extraction stops unexpectedly,
             * the index still contains everything processed before
             * the failure.
             */
            messageIndex.save();

            if (!parsed.messageIds().isEmpty()) {

                // Page 1 contains the newest messages.
                if (newestMessageId == null) {

                    newestMessageId =
                            parsed.messageIds().get(0);

                    newestTimestampMs =
                            parsed.newestTimestampMs();
                }

                // Every subsequent page moves further into history.
                oldestMessageId =
                        parsed.messageIds()
                                .get(
                                        parsed.messageIds().size() - 1
                                );

                oldestTimestampMs =
                        parsed.oldestTimestampMs();
            }

            String firstMessageId =
                    first(parsed);

            String lastMessageId =
                    last(parsed);

            manifest.addPage(
                    new ExtractionManifest.PageMetadata(
                            page,
                            savedFile.getFileName()
                                    .toString(),
                            parsed.nodeCount(),
                            firstMessageId,
                            lastMessageId,
                            parsed.endCursor(),
                            parsed.hasNextPage()
                    )
            );

            /*
             * Save manifest after every page.
             *
             * If the program crashes after page 3,
             * the manifest still knows that page 1-3
             * were successfully persisted.
             */
            store.saveManifest(
                    manifest
            );

            System.out.println(
                    "Nodes: " +
                    parsed.nodeCount()
            );

            System.out.println(
                    "First message ID: " +
                    firstMessageId
            );

            System.out.println(
                    "Last message ID: " +
                    lastMessageId
            );

            System.out.println(
                    "Has next page: " +
                    parsed.hasNextPage()
            );

            System.out.println(
                    "End cursor: " +
                    parsed.endCursor()
            );

            System.out.println(
                    "Saved: " +
                    savedFile
            );

            System.out.println(
                    "Index size: " +
                    messageIndex.size()
            );

            /*
             * No more data.
             */
            if (!parsed.hasNextPage()) {

                stopReason =
                        "NO_MORE_PAGES";

                break;
            }

            /*
             * Instagram says there is another page,
             * but didn't provide a cursor.
             */
            if (parsed.endCursor() == null ||
                    parsed.endCursor().isBlank()) {

                stopReason =
                        "MISSING_END_CURSOR";

                break;
            }

            /*
             * We reached our configured diagnostic
             * limit.
             */
            if (page == config.maxPages()) {

                stopReason =
                        "MAX_PAGES_REACHED";

                break;
            }

            afterCursor =
                    parsed.endCursor();

            page++;
        }

        manifest.setCompletedAt(
                OffsetDateTime.now(
                        ZoneOffset.systemDefault()
                )
        );

        manifest.setStopReason(
                stopReason
        );

        manifest.setHasMorePages(
                "MAX_PAGES_REACHED"
                        .equals(stopReason)
                        || "UNKNOWN"
                        .equals(stopReason)
                        && manifest.isHasMorePages()
        );

        manifest.setStatus(
                "MAX_PAGES_REACHED"
                        .equals(stopReason)
                        ? "PARTIAL"
                        : "COMPLETE"
        );

        store.saveManifest(
                manifest
        );

        System.out.println();
        System.out.println(
                "========================================"
        );

        if ("PARTIAL".equals(
                manifest.getStatus())) {

            System.out.println(
                    "EXTRACTION STOPPED"
            );

            System.out.println(
                    "Reason: " +
                    manifest.getStopReason()
            );

            System.out.println(
                    "More pages available: " +
                    manifest.isHasMorePages()
            );

        } else {

            System.out.println(
                    "EXTRACTION COMPLETE"
            );
        }

        System.out.println(
                "Pages downloaded: " +
                manifest.getPagesDownloaded()
        );

        System.out.println(
                "Messages indexed: " +
                messageIndex.size()
        );

        System.out.println(
                "Manifest: " +
                store.getManifestFile()
        );

        System.out.println(
                "Archive: " +
                store.getSyncDirectory()
        );

        System.out.println(
                "========================================"
        );

        ExtractionState state =
                new ExtractionState(
                        config.conversationId()
                );

        state.setTotalMessages(totalMessages);

        if (newestMessageId != null) {

            state.setNewestMessage(
                    new ExtractionState.MessageBoundary(
                            newestMessageId,
                            newestTimestampMs
                    )
            );
        }

        if (oldestMessageId != null) {

            state.setOldestMessage(
                    new ExtractionState.MessageBoundary(
                            oldestMessageId,
                            oldestTimestampMs
                    )
            );
        }

        state.setLastSync(
                new ExtractionState.SyncInfo(
                        store.getSyncDirectory()
                                .getFileName()
                                .toString(),
                        manifest.getCompletedAt(),
                        manifest.getStatus()
                )
        );

        stateStore.save(state);
    }

    private String first(
            InstagramResponseParser.ParsedResponse response) {

        return response.messageIds().isEmpty()
                ? "<none>"
                : response.messageIds().get(0);
    }

    private String last(
            InstagramResponseParser.ParsedResponse response) {

        return response.messageIds().isEmpty()
                ? "<none>"
                : response.messageIds()
                        .get(
                                response.messageIds().size() - 1
                        );
    }
}