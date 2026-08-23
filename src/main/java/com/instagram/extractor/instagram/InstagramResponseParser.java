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

                if (!id.isBlank()) {
                    messageIds.add(id);
                }
            }
        }

        String endCursor =
                pageInfo.path("end_cursor")
                        .asText("");

        boolean hasNextPage =
                pageInfo.path("has_next_page")
                        .asBoolean(false);

        return new ParsedResponse(
                edges.size(),
                messageIds,
                endCursor,
                hasNextPage
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
            String endCursor,
            boolean hasNextPage
    ) {
    }
}