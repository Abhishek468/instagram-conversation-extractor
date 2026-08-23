package com.instagram.extractor.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.instagram.extractor.instagram.InstagramResponseParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JsonMessageIndex
        implements MessageIndex {

    private final Path conversationDirectory;
    private final Path indexFile;
    private final ObjectMapper objectMapper;

    private final Map<String, MessageLocation> locations;

    public JsonMessageIndex(
            Path conversationDirectory,
            ObjectMapper objectMapper,
            InstagramResponseParser parser)
            throws IOException {

        Files.createDirectories(
                conversationDirectory
        );

        this.conversationDirectory =
                conversationDirectory;

        this.indexFile =
                conversationDirectory.resolve(
                        "message-index.json"
                );

        this.objectMapper =
                objectMapper.copy()
                        .enable(
                                SerializationFeature.INDENT_OUTPUT
                        );

        this.locations =
                new HashMap<>();

        if (Files.exists(indexFile)) {

            loadExistingIndex(
                    parser
            );
        }
    }

    private void loadExistingIndex(
            InstagramResponseParser parser)
            throws IOException {

        JsonNode root =
                objectMapper.readTree(
                        indexFile.toFile()
                );

        /*
         * ============================================================
         * NEW FORMAT
         * ============================================================
         *
         * {
         *   "message-id": {
         *      "syncId": "...",
         *      "page": "..."
         *   }
         * }
         */

        if (root.isObject()) {

            var fields =
                    root.fields();

            while (fields.hasNext()) {

                var entry =
                        fields.next();

                String messageId =
                        entry.getKey();

                JsonNode locationNode =
                        entry.getValue();

                String syncId =
                        locationNode
                                .path("syncId")
                                .asText("");

                String page =
                        locationNode
                                .path("page")
                                .asText("");

                if (!syncId.isBlank()
                        && !page.isBlank()) {

                    locations.put(
                            messageId,
                            new MessageLocation(
                                    syncId,
                                    page
                            )
                    );
                }
            }

            return;
        }

        /*
         * ============================================================
         * LEGACY FORMAT
         * ============================================================
         *
         * [
         *   "message-id-1",
         *   "message-id-2"
         * ]
         *
         * We migrate this automatically by scanning the existing
         * raw archive.
         */

        if (root.isArray()) {

            Set<String> legacyIds =
                    objectMapper.convertValue(
                            root,
                            new TypeReference<Set<String>>() {}
                    );

            migrateLegacyIndex(
                    legacyIds,
                    parser
            );

            save();
        }
    }

    private void migrateLegacyIndex(
            Set<String> legacyIds,
            InstagramResponseParser parser)
            throws IOException {

        if (legacyIds.isEmpty()) {
            return;
        }

        System.out.println();
        System.out.println(
                "LEGACY MESSAGE INDEX DETECTED"
        );

        System.out.println(
                "Migrating " +
                        legacyIds.size() +
                        " message IDs to location-aware index."
        );

        List<Path> pageFiles =
                discoverPageFiles();

        int locationsFound = 0;

        for (Path pageFile : pageFiles) {

            try {

                String rawJson =
                        Files.readString(
                                pageFile
                        );

                InstagramResponseParser
                        .ParsedResponse parsed =
                        parser.parse(rawJson);

                String syncId =
                        pageFile
                                .getParent()
                                .getFileName()
                                .toString();

                String page =
                        pageFile
                                .getFileName()
                                .toString();

                for (String messageId :
                        parsed.messageIds()) {

                    if (!legacyIds.contains(
                            messageId)) {

                        continue;
                    }

                    if (locations.containsKey(
                            messageId)) {

                        continue;
                    }

                    locations.put(
                            messageId,
                            new MessageLocation(
                                    syncId,
                                    page
                            )
                    );

                    locationsFound++;
                }

            } catch (Exception e) {

                throw new IOException(
                        "Failed to migrate archive page: "
                                + pageFile,
                        e
                );
            }
        }

        System.out.println(
                "Locations recovered: " +
                        locationsFound +
                        "/" +
                        legacyIds.size()
        );

        if (locationsFound
                != legacyIds.size()) {

            throw new IOException(
                    "Could not locate all legacy message IDs. "
                            + "Expected " +
                            legacyIds.size() +
                            " but found " +
                            locationsFound
            );
        }

        System.out.println(
                "Legacy index migration complete."
        );
    }

    private List<Path> discoverPageFiles()
            throws IOException {

        List<Path> pages =
                new ArrayList<>();

        if (!Files.exists(
                conversationDirectory)) {

            return pages;
        }

        try (var syncDirectories =
                     Files.list(
                             conversationDirectory
                     )) {

            syncDirectories
                    .filter(Files::isDirectory)
                    .forEach(syncDirectory -> {

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

                        } catch (IOException e) {

                            throw new RuntimeException(
                                    "Failed to read sync directory: "
                                            + syncDirectory,
                                    e
                            );
                        }
                    });
        }

        return pages;
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

    @Override
    public boolean contains(
            String messageId) {

        return locations.containsKey(
                messageId
        );
    }

    @Override
    public void add(
            String messageId,
            MessageLocation location) {

        if (messageId == null
                || messageId.isBlank()) {

            return;
        }

        if (location == null) {
            return;
        }

        /*
         * Do not overwrite an existing location.
         *
         * The first location is sufficient to retrieve
         * the message, and preserving it gives us a stable
         * archive reference.
         */
        locations.putIfAbsent(
                messageId,
                location
        );
    }

    @Override
    public Optional<MessageLocation> getLocation(
            String messageId) {

        return Optional.ofNullable(
                locations.get(messageId)
        );
    }

    @Override
    public int size() {

        return locations.size();
    }

    @Override
    public void save()
            throws IOException {

        objectMapper.writeValue(
                indexFile.toFile(),
                locations
        );
    }

    public Path getIndexFile() {

        return indexFile;
    }
}