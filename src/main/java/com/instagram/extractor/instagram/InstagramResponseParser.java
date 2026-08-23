package com.instagram.extractor.instagram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class InstagramResponseParser {

    private final ObjectMapper objectMapper;

    public InstagramResponseParser(
            ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    public ParsedResponse parse(
            String rawJson) throws Exception {

        JsonNode root =
                objectMapper.readTree(rawJson);

        JsonNode slideMessages =
                findSlideMessages(root);

        if (slideMessages == null) {

            throw new IllegalStateException(
                    "slide_messages not found in response"
            );
        }

        JsonNode edges =
                slideMessages.path("edges");

        JsonNode pageInfo =
                slideMessages.path("page_info");

        List<String> messageIds =
                new ArrayList<>();

        List<Long> timestamps =
                new ArrayList<>();

        List<ParsedMessage> messages =
                new ArrayList<>();

        if (edges.isArray()) {

            for (JsonNode edge : edges) {

                JsonNode node =
                        edge.path("node");

                String id =
                        node.path("message_id")
                                .asText("");

                if (id.isBlank()) {

                    id =
                            node.path("id")
                                    .asText("");
                }

                long timestampMs = 0;

                JsonNode timestampNode =
                        node.get("timestamp_ms");

                if (timestampNode != null
                        && !timestampNode.isNull()) {

                    timestampMs =
                            timestampNode.asLong();

                    timestamps.add(timestampMs);
                }

                if (!id.isBlank()) {

                    messageIds.add(id);

                    messages.add(
                            new ParsedMessage(
                                    id,
                                    timestampMs,
                                    node.deepCopy()
                            )
                    );
                }
            }
        }

        String endCursor =
                pageInfo.path("end_cursor")
                        .asText("");

        boolean hasNextPage =
                pageInfo.path("has_next_page")
                        .asBoolean(false);

        long oldestTimestampMs =
                timestamps.isEmpty()
                        ? 0
                        : timestamps.stream()
                                .mapToLong(Long::longValue)
                                .min()
                                .orElse(0);

        long newestTimestampMs =
                timestamps.isEmpty()
                        ? 0
                        : timestamps.stream()
                                .mapToLong(Long::longValue)
                                .max()
                                .orElse(0);

        return new ParsedResponse(
                edges.size(),
                messageIds,
                messages,
                endCursor,
                hasNextPage,
                oldestTimestampMs,
                newestTimestampMs
        );
    }

    private JsonNode findSlideMessages(
            JsonNode node) {

        if (node == null) {
            return null;
        }

        if (node.isObject()) {

            JsonNode slide =
                    node.get("slide_messages");

            if (slide != null &&
                    slide.isObject()) {

                return slide;
            }

            var fields = node.fields();

            while (fields.hasNext()) {

                JsonNode result =
                        findSlideMessages(
                                fields.next().getValue()
                        );

                if (result != null) {
                    return result;
                }
            }
        }

        if (node.isArray()) {

            for (JsonNode child : node) {

                JsonNode result =
                        findSlideMessages(child);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    public record ParsedResponse(
            int nodeCount,
            List<String> messageIds,
            List<ParsedMessage> messages,
            String endCursor,
            boolean hasNextPage,
            long oldestTimestampMs,
            long newestTimestampMs
    ) {
    }

    public record ParsedMessage(
            String id,
            long timestampMs,
            JsonNode rawNode
    ) {
    }
}