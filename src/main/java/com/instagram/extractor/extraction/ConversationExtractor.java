package com.instagram.extractor.extraction;

import com.instagram.extractor.config.InstagramConfig;
import com.instagram.extractor.instagram.InstagramClient;
import com.instagram.extractor.instagram.InstagramResponseParser;
import com.instagram.extractor.storage.ExtractionManifest;
import com.instagram.extractor.storage.RawResponseStore;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class ConversationExtractor {

    private final InstagramConfig config;
    private final InstagramClient client;
    private final InstagramResponseParser parser;
    private final RawResponseStore store;

    public ConversationExtractor(
            InstagramConfig config,
            InstagramClient client,
            InstagramResponseParser parser,
            RawResponseStore store) {

        this.config = config;
        this.client = client;
        this.parser = parser;
        this.store = store;
    }

    public void extract() throws Exception {

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