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
import java.util.HashSet;
import java.util.Set;

public class SyncEngine {

    private final InstagramConfig config;
    private final InstagramClient client;
    private final InstagramResponseParser parser;
    private final RawResponseStore store;
    private final StateStore stateStore;
    private final MessageIndex messageIndex;

    public SyncEngine(
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

    public void sync() throws Exception {

        System.out.println();
        System.out.println(
                "========================================"
        );
        System.out.println(
                "STARTING INSTAGRAM SYNC"
        );
        System.out.println(
                "Conversation: " +
                config.conversationId()
        );
        System.out.println(
                "Existing indexed messages: " +
                messageIndex.size()
        );
        System.out.println(
                "========================================"
        );

        ExtractionState existingState =
                stateStore.load();

        if (existingState == null) {

            throw new IllegalStateException(
                    "Sync requested but state.json could not be loaded."
            );
        }

        /*
         * We create a completely new sync archive.
         *
         * The old archive is never modified.
         */
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

        int totalNodes = 0;
        int newMessages = 0;
        int knownMessages = 0;

        String newestMessageId = null;
        long newestTimestampMs = 0;

        String oldestMessageId = null;
        long oldestTimestampMs = Long.MAX_VALUE;

        String stopReason =
                "UNKNOWN";

        boolean boundaryReached = false;

        while (page <= config.maxPages()) {

            System.out.println();
            System.out.println(
                    "REQUESTING SYNC PAGE " + page
            );

            /*
             * null cursor means:
             *
             * "start from the newest messages"
             */
            String rawJson =
                    client.fetchPage(afterCursor);

            /*
             * IMPORTANT:
             *
             * Save the raw response before doing anything
             * with the parsed representation.
             */
            Path savedFile =
                    store.save(
                            page,
                            rawJson
                    );

            InstagramResponseParser.ParsedResponse
                    parsed =
                    parser.parse(rawJson);

            totalNodes += parsed.nodeCount();

            boolean pageContainsKnownMessage =
                    false;

            Set<String> pageNewIds =
                    new HashSet<>();

            for (String messageId :
                    parsed.messageIds()) {

                if (messageIndex.contains(messageId)) {

                    knownMessages++;
                    pageContainsKnownMessage = true;

                } else {

                    pageNewIds.add(messageId);
                }
            }

            /*
             * Add only genuinely new IDs.
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

for (String messageId : pageNewIds) {

    messageIndex.add(
            messageId,
            location
    );

    newMessages++;
}

            /*
             * Persist the index after processing this page.
             */
            messageIndex.save();

            /*
             * Track message boundaries for the new sync.
             */
            if (!parsed.messageIds().isEmpty()) {

                if (newestMessageId == null) {

                    newestMessageId =
                            parsed.messageIds().get(0);

                    newestTimestampMs =
                            parsed.newestTimestampMs();
                }

                oldestMessageId =
                        parsed.messageIds().get(
                                parsed.messageIds().size() - 1
                        );

                oldestTimestampMs =
                        parsed.oldestTimestampMs();
            }

            manifest.addPage(
                    new ExtractionManifest.PageMetadata(
                            page,
                            savedFile.getFileName()
                                    .toString(),
                            parsed.nodeCount(),
                            first(parsed),
                            last(parsed),
                            parsed.endCursor(),
                            parsed.hasNextPage()
                    )
            );

            store.saveManifest(manifest);

            System.out.println(
                    "Nodes: " +
                    parsed.nodeCount()
            );

            System.out.println(
                    "New messages: " +
                    pageNewIds.size()
            );

            System.out.println(
                    "Known messages: " +
                    (
                            parsed.nodeCount()
                                    - pageNewIds.size()
                    )
            );

            System.out.println(
                    "Index size: " +
                    messageIndex.size()
            );

            System.out.println(
                    "First message ID: " +
                    first(parsed)
            );

            System.out.println(
                    "Last message ID: " +
                    last(parsed)
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

            /*
             * ====================================================
             * BOUNDARY DETECTED
             * ====================================================
             *
             * This page contains both:
             *
             *     new messages
             *     +
             *     messages already in our archive
             *
             * Therefore we have reached the existing archive.
             *
             * We have already persisted this complete page, so
             * no data is lost.
             */
            if (pageContainsKnownMessage) {

                boundaryReached = true;

                stopReason =
                        "EXISTING_ARCHIVE_BOUNDARY_REACHED";

                System.out.println();
                System.out.println(
                        "ARCHIVE BOUNDARY REACHED"
                );

                System.out.println(
                        "Known message detected on page " +
                        page
                );

                break;
            }

            /*
             * No next page.
             */
            if (!parsed.hasNextPage()) {

                stopReason =
                        "NO_MORE_PAGES";

                break;
            }

            /*
             * Instagram says another page exists,
             * but did not provide a cursor.
             */
            if (parsed.endCursor() == null ||
                    parsed.endCursor().isBlank()) {

                stopReason =
                        "MISSING_END_CURSOR";

                break;
            }

            /*
             * We reached the configured safety limit.
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

        /*
         * ========================================================
         * FINALIZE MANIFEST
         * ========================================================
         */

        manifest.setCompletedAt(
                OffsetDateTime.now(
                        ZoneOffset.systemDefault()
                )
        );

        manifest.setStopReason(
                stopReason
        );

        manifest.setHasMorePages(
                !boundaryReached
                        && "MAX_PAGES_REACHED"
                        .equals(stopReason)
        );

        /*
         * A sync that reaches the existing archive boundary
         * is considered successful.
         *
         * It doesn't mean that Instagram has no more history.
         * It means that our local archive is caught up to the
         * point that existed before this sync.
         */
        manifest.setStatus(
                "EXISTING_ARCHIVE_BOUNDARY_REACHED"
                        .equals(stopReason)
                        ? "COMPLETE"
                        : "MAX_PAGES_REACHED"
                                .equals(stopReason)
                        ? "PARTIAL"
                        : "COMPLETE"
        );

        store.saveManifest(manifest);

        /*
         * ========================================================
         * UPDATE STATE
         * ========================================================
         */

        ExtractionState newState =
                new ExtractionState(
                        config.conversationId()
                );

        /*
         * Existing state + newly discovered messages.
         *
         * For the POC we derive the new total from the
         * persistent message index.
         */
        newState.setTotalMessages(
                messageIndex.size()
        );

        if (newestMessageId != null) {

            newState.setNewestMessage(
                    new ExtractionState.MessageBoundary(
                            newestMessageId,
                            newestTimestampMs
                    )
            );
        } else {

            newState.setNewestMessage(
                    existingState.getNewestMessage()
            );
        }

        /*
         * The oldest boundary remains the old archive boundary
         * when this sync only brings newer messages.
         *
         * Therefore we preserve the existing oldest message.
         */
        newState.setOldestMessage(
                existingState.getOldestMessage()
        );

        newState.setLastSync(
                new ExtractionState.SyncInfo(
                        store.getSyncDirectory()
                                .getFileName()
                                .toString(),
                        manifest.getCompletedAt(),
                        manifest.getStatus()
                )
        );

        stateStore.save(newState);

        /*
         * ========================================================
         * SUMMARY
         * ========================================================
         */

        System.out.println();
        System.out.println(
                "========================================"
        );
        System.out.println(
                "SYNC COMPLETE"
        );
        System.out.println(
                "========================================"
        );

        System.out.println(
                "Pages downloaded: " +
                manifest.getPagesDownloaded()
        );

        System.out.println(
                "Messages in sync responses: " +
                totalNodes
        );

        System.out.println(
                "New messages discovered: " +
                newMessages
        );

        System.out.println(
                "Known messages encountered: " +
                knownMessages
        );

        System.out.println(
                "Total indexed messages: " +
                messageIndex.size()
        );

        System.out.println(
                "Stop reason: " +
                stopReason
        );

        System.out.println(
                "Archive boundary reached: " +
                boundaryReached
        );

        System.out.println(
                "Sync archive: " +
                store.getSyncDirectory()
        );

        System.out.println(
                "========================================"
        );
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
                : response.messageIds().get(
                        response.messageIds().size() - 1
                );
    }
}