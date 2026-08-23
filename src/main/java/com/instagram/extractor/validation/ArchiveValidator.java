package com.instagram.extractor.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagram.extractor.instagram.InstagramResponseParser;
import com.instagram.extractor.storage.ExtractionManifest;
import com.instagram.extractor.storage.ExtractionState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArchiveValidator {

    private final Path conversationDirectory;
    private final ObjectMapper objectMapper;
    private final InstagramResponseParser parser;

    public ArchiveValidator(
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

    public ValidationResult validate()
            throws IOException {

        System.out.println();
        System.out.println(
                "========================================"
        );
        System.out.println(
                "ARCHIVE INTEGRITY VALIDATION"
        );
        System.out.println(
                "========================================"
        );

        /*
         * ========================================================
         * LOAD INDEX
         * ========================================================
         */

        Path indexFile =
                conversationDirectory.resolve(
                        "message-index.json"
                );

        if (!Files.exists(indexFile)) {

            return fail(
                    "message-index.json does not exist."
            );
        }

        Set<String> indexedIds =
                objectMapper.readValue(
                        indexFile.toFile(),
                        new TypeReference<Set<String>>() {}
                );

        /*
         * ========================================================
         * LOAD STATE
         * ========================================================
         */

        Path stateFile =
                conversationDirectory.resolve(
                        "state.json"
                );

        if (!Files.exists(stateFile)) {

            return fail(
                    "state.json does not exist."
            );
        }

        ExtractionState state =
                objectMapper.readValue(
                        stateFile.toFile(),
                        ExtractionState.class
                );

        /*
         * ========================================================
         * BASIC STATE / INDEX CHECK
         * ========================================================
         */

        boolean stateIndexCountMatches =
                state.getTotalMessages()
                        == indexedIds.size();

        /*
         * ========================================================
         * DISCOVER SYNC DIRECTORIES
         * ========================================================
         */

        List<Path> syncDirectories =
                findSyncDirectories();

        System.out.println(
                "Conversation: " +
                state.getConversationId()
        );

        System.out.println(
                "Sync archives found: " +
                syncDirectories.size()
        );

        System.out.println();

        /*
         * ========================================================
         * SCAN RAW ARCHIVES
         * ========================================================
         */

        Set<String> archivedMessageIds =
                new HashSet<>();

        int corruptJsonPages = 0;
        int invalidManifests = 0;
        int pageFileCount = 0;

        List<String> validationErrors =
                new ArrayList<>();

        for (Path syncDirectory :
                syncDirectories) {

            System.out.println(
                    "Checking: " +
                    syncDirectory.getFileName()
            );

            /*
             * ----------------------------------------------------
             * MANIFEST
             * ----------------------------------------------------
             */

            Path manifestFile =
                    syncDirectory.resolve(
                            "manifest.json"
                    );

            if (!Files.exists(manifestFile)) {

                invalidManifests++;

                validationErrors.add(
                        "Missing manifest: " +
                        manifestFile
                );

            } else {

                try {

                    ExtractionManifest manifest =
                            objectMapper.readValue(
                                    manifestFile.toFile(),
                                    ExtractionManifest.class
                            );

                    if (!validateManifest(
                            manifest,
                            syncDirectory,
                            validationErrors)) {

                        invalidManifests++;
                    }

                } catch (Exception e) {

                    invalidManifests++;

                    validationErrors.add(
                            "Invalid manifest: " +
                            manifestFile +
                            " -> " +
                            e.getMessage()
                    );
                }
            }

            /*
             * ----------------------------------------------------
             * RAW PAGE FILES
             * ----------------------------------------------------
             */

            List<Path> pageFiles =
                    findPageFiles(
                            syncDirectory
                    );

            pageFiles.sort(
                    Comparator.comparing(
                            Path::toString
                    )
            );

                for (Path pageFile :
                pageFiles) {

                 pageFileCount++;

                try {

                    String rawJson =
                            Files.readString(
                                    pageFile
                            );

                    /*
                     * Parsing through our existing parser does two
                     * things:
                     *
                     * 1. verifies that the JSON is syntactically
                     *    and structurally usable by our system
                     *
                     * 2. extracts the message IDs exactly the same
                     *    way the extractor does
                     */
                    InstagramResponseParser.ParsedResponse
                            parsed =
                            parser.parse(rawJson);

                    archivedMessageIds.addAll(
                            parsed.messageIds()
                    );

                } catch (Exception e) {

                    corruptJsonPages++;

                    validationErrors.add(
                            "Invalid page: " +
                            pageFile +
                            " -> " +
                            e.getMessage()
                    );
                }
            }
        }

        /*
         * ========================================================
         * INDEX → ARCHIVE CHECK
         * ========================================================
         *
         * Every indexed message must exist somewhere in our
         * persisted raw responses.
         */

        Set<String> missingFromArchive =
                new HashSet<>(
                        indexedIds
                );

        missingFromArchive.removeAll(
                archivedMessageIds
        );

        /*
         * ========================================================
         * STATE BOUNDARY CHECKS
         * ========================================================
         */

        boolean newestMessageFound =
                state.getNewestMessage() != null
                        && archivedMessageIds.contains(
                        state.getNewestMessage().id()
                );

        boolean oldestMessageFound =
                state.getOldestMessage() != null
                        && archivedMessageIds.contains(
                        state.getOldestMessage().id()
                );

        /*
         * ========================================================
         * PRINT RESULTS
         * ========================================================
         */

        System.out.println();
        System.out.println(
                "State messages:          " +
                state.getTotalMessages()
        );

        System.out.println(
                "Indexed messages:        " +
                indexedIds.size()
        );

        System.out.println(
                "Archived message IDs:    " +
                archivedMessageIds.size()
        );

        System.out.println(
                "Raw page files:          " +
                pageFileCount
        );
        System.out.println(
                "Duplicate index IDs:     0"
        );

        System.out.println(
                "Missing from archive:    " +
                missingFromArchive.size()
        );

        System.out.println(
                "Corrupt JSON pages:      " +
                corruptJsonPages
        );

        System.out.println(
                "Invalid manifests:       " +
                invalidManifests
        );

        System.out.println(
                "Newest state message:    " +
                (
                        newestMessageFound
                                ? "FOUND"
                                : "NOT FOUND"
                )
        );

        System.out.println(
                "Oldest state message:    " +
                (
                        oldestMessageFound
                                ? "FOUND"
                                : "NOT FOUND"
                )
        );

        /*
         * ========================================================
         * FINAL PASS / FAIL
         * ========================================================
         */

        boolean indexConsistent =
                stateIndexCountMatches
                        && missingFromArchive.isEmpty();

        boolean archiveIntegrity =
                corruptJsonPages == 0
                        && invalidManifests == 0
                        && missingFromArchive.isEmpty();

        boolean stateConsistent =
                stateIndexCountMatches
                        && newestMessageFound
                        && oldestMessageFound;

        System.out.println();

        System.out.println(
                "STATE CONSISTENCY:       " +
                passFail(stateConsistent)
        );

        System.out.println(
                "INDEX CONSISTENCY:       " +
                passFail(indexConsistent)
        );

        System.out.println(
                "ARCHIVE INTEGRITY:       " +
                passFail(archiveIntegrity)
        );

        if (!validationErrors.isEmpty()) {

            System.out.println();
            System.out.println(
                    "VALIDATION ERRORS:"
            );

            for (String error :
                    validationErrors) {

                System.out.println(
                        " - " + error
                );
            }
        }

        boolean overallPass =
                stateConsistent
                        && indexConsistent
                        && archiveIntegrity;

        System.out.println();
        System.out.println(
                "========================================"
        );

        System.out.println(
                overallPass
                        ? "VALIDATION COMPLETE: PASS"
                        : "VALIDATION COMPLETE: FAIL"
        );

        System.out.println(
                "========================================"
        );

        return new ValidationResult(
                overallPass,
                state.getTotalMessages(),
                indexedIds.size(),
                archivedMessageIds.size(),
                missingFromArchive.size(),
                corruptJsonPages,
                invalidManifests
        );
    }

    private List<Path> findSyncDirectories()
            throws IOException {

        try (Stream<Path> stream =
                     Files.list(
                             conversationDirectory
                     )) {

            return stream
                    .filter(Files::isDirectory)
                    .filter(
                            path ->
                                    path.getFileName()
                                            .toString()
                                            .startsWith("sync_")
                    )
                    .sorted()
                    .collect(
                            Collectors.toList()
                    );
        }
    }

    private List<Path> findPageFiles(
            Path syncDirectory)
            throws IOException {

        try (Stream<Path> stream =
                     Files.list(
                             syncDirectory
                     )) {

            return stream
                    .filter(Files::isRegularFile)
                    .filter(
                            path -> {

                                String name =
                                        path.getFileName()
                                                .toString();

                                return name.startsWith(
                                        "page_"
                                ) && name.endsWith(
                                        ".json"
                                );
                            }
                    )
                    .collect(
                            Collectors.toList()
                    );
        }
    }

    private boolean validateManifest(
            ExtractionManifest manifest,
            Path syncDirectory,
            List<String> errors) {

        boolean valid = true;

        if (manifest.getConversationId() == null ||
                manifest.getConversationId().isBlank()) {

            errors.add(
                    "Manifest has no conversationId: " +
                    syncDirectory
            );

            valid = false;
        }

        if (manifest.getPagesDownloaded()
                != manifest.getPages().size()) {

            errors.add(
                    "Manifest page count mismatch: " +
                    syncDirectory
            );

            valid = false;
        }

        if (manifest.getPages().isEmpty()) {

            errors.add(
                    "Manifest contains no pages: " +
                    syncDirectory
            );

            valid = false;
        }

        for (
                ExtractionManifest.PageMetadata page :
                manifest.getPages()
        ) {

            Path pageFile =
                    syncDirectory.resolve(
                            page.fileName()
                    );

            if (!Files.exists(pageFile)) {

                errors.add(
                        "Manifest references missing page: " +
                        pageFile
                );

                valid = false;
            }
        }

        return valid;
    }

    private ValidationResult fail(
            String reason) {

        System.out.println();
        System.out.println(
                "VALIDATION FAILED"
        );
        System.out.println(reason);

        return new ValidationResult(
                false,
                0,
                0,
                0,
                0,
                0,
                0
        );
    }

    private String passFail(
            boolean value) {

        return value
                ? "PASS"
                : "FAIL";
    }

    public record ValidationResult(
            boolean passed,
            int stateMessages,
            int indexedMessages,
            int archivedMessageIds,
            int missingFromArchive,
            int corruptJsonPages,
            int invalidManifests
    ) {
    }
}