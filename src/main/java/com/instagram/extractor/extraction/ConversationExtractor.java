package com.instagram.extractor.extraction;

import com.instagram.extractor.config.InstagramConfig;
import com.instagram.extractor.instagram.InstagramClient;
import com.instagram.extractor.instagram.InstagramResponseParser;
import com.instagram.extractor.storage.RawResponseStore;

import java.nio.file.Path;

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

        String afterCursor = null;

        int page = 1;

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
                    client.fetchPage(afterCursor);

            Path savedFile =
                    store.save(
                            page,
                            rawJson
                    );

            InstagramResponseParser.ParsedResponse
                    parsed =
                    parser.parse(rawJson);

            System.out.println(
                    "Nodes: " +
                    parsed.nodeCount()
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

            if (!parsed.hasNextPage()) {

                System.out.println();
                System.out.println(
                        "Instagram reports no more pages."
                );

                break;
            }

            if (parsed.endCursor() == null ||
                    parsed.endCursor().isBlank()) {

                throw new IllegalStateException(
                        "has_next_page=true but " +
                        "end_cursor is empty"
                );
            }

            afterCursor =
                    parsed.endCursor();

            page++;
        }

        System.out.println();
        System.out.println(
                "========================================"
        );
        System.out.println(
                "EXTRACTION COMPLETE"
        );
        System.out.println(
                "Pages downloaded: " + (page <= config.maxPages()
                        ? page
                        : config.maxPages())
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
                        .get(response.messageIds().size() - 1);
    }
}