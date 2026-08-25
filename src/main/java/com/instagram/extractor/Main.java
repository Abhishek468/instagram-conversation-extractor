package com.instagram.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.instagram.extractor.config.InstagramConfig;
import com.instagram.extractor.extraction.ConversationExtractor;
import com.instagram.extractor.extraction.SyncEngine;
import com.instagram.extractor.instagram.GraphQLRequestBuilder;
import com.instagram.extractor.instagram.InstagramClient;
import com.instagram.extractor.instagram.InstagramResponseParser;
import com.instagram.extractor.storage.JsonMessageCatalog;
import com.instagram.extractor.storage.JsonMessageIndex;
import com.instagram.extractor.storage.MessageCatalog;
import com.instagram.extractor.storage.MessageIndex;
import com.instagram.extractor.storage.RawResponseStore;
import com.instagram.extractor.storage.StateStore;
import com.instagram.extractor.validation.ArchiveValidator;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {

        try {

            InstagramConfig config =
                    new InstagramConfig();

            ObjectMapper objectMapper =
                    new ObjectMapper();

            objectMapper.registerModule(
                    new JavaTimeModule()
            );

            /*
             * ====================================================
             * VALIDATION MODE
             * ====================================================
             *
             * Run with:
             *
             *     validate
             *
             * Validation must remain completely read-only.
             *
             * IMPORTANT:
             * Do NOT create RawResponseStore here because its
             * constructor creates a new sync directory.
             */

            if (args.length > 0 &&
                    "validate".equalsIgnoreCase(args[0])) {

       
                InstagramResponseParser parser =
                        new InstagramResponseParser(
                                objectMapper
                        );

                Path conversationDirectory =
                        config.outputDirectory()
                                .resolve(
                                        config.conversationId()
                                );

                ArchiveValidator validator =
                        new ArchiveValidator(
                                conversationDirectory,
                                objectMapper,
                                parser
                        );

                ArchiveValidator.ValidationResult
                        result =
                        validator.validate();

                if (!result.passed()) {
                    System.exit(1);
                }

                return;
            }

            /*
             * ====================================================
             * NORMAL EXTRACTION / SYNC MODE
             * ====================================================
             */

            GraphQLRequestBuilder requestBuilder =
                    new GraphQLRequestBuilder(
                            config,
                            objectMapper
                    );

            InstagramClient client =
                    new InstagramClient(
                            config,
                            requestBuilder
                    );

            InstagramResponseParser parser =
                    new InstagramResponseParser(
                            objectMapper
                    );

            /*
             * RawResponseStore is intentionally created only
             * after validation mode has been ruled out.
             *
             * Its constructor creates a new sync directory.
             */

            RawResponseStore store =
                    new RawResponseStore(
                            config.outputDirectory(),
                            config.conversationId(),
                            objectMapper
                    );

                    MessageIndex messageIndex =
        new JsonMessageIndex(
                store.getConversationDirectory(),
                objectMapper,
                parser
        );

            MessageCatalog messageCatalog =
                    new JsonMessageCatalog(
                            store.getConversationDirectory(),
                            objectMapper
                    );

            StateStore stateStore =
                    new StateStore(
                            store.getConversationDirectory(),
                            objectMapper
                    );

            /*
             * ====================================================
             * FIRST RUN / SUBSEQUENT RUN
             * ====================================================
             *
             * First run:
             *
             *     no state.json
             *          ↓
             *     full extraction
             *
             * Subsequent run:
             *
             *     state.json exists
             *          ↓
             *     incremental sync
             */

            if (!stateStore.exists()) {

                System.out.println();
                System.out.println(
                        "NO EXISTING STATE FOUND"
                );

                System.out.println(
                        "Starting INITIAL EXTRACTION."
                );

                ConversationExtractor extractor =
                        new ConversationExtractor(
                                config,
                                client,
                                parser,
                                store,
                                stateStore,
                                messageIndex,
                                messageCatalog
                        );

                extractor.extract();

            } else {

                System.out.println();
                System.out.println(
                        "EXISTING STATE FOUND"
                );

                System.out.println(
                        "Starting INCREMENTAL SYNC."
                );

                SyncEngine syncEngine =
                        new SyncEngine(
                                config,
                                client,
                                parser,
                                store,
                                stateStore,
                                messageIndex
                        );

                syncEngine.sync();
            }

        } catch (Exception e) {

            System.err.println();
            System.err.println(
                    "EXTRACTION / SYNC FAILED"
            );

            e.printStackTrace();

            System.exit(1);
        }
    }
}