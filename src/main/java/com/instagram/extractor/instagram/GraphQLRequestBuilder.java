package com.instagram.extractor.instagram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.instagram.extractor.config.InstagramConfig;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class GraphQLRequestBuilder {

    private final InstagramConfig config;
    private final ObjectMapper objectMapper;

    public GraphQLRequestBuilder(
            InstagramConfig config,
            ObjectMapper objectMapper) {

        this.config = config;
        this.objectMapper = objectMapper;
    }

    public String buildVariables(String afterCursor)
            throws Exception {

        ObjectNode variables =
                objectMapper.createObjectNode();

        /*
         * IMPORTANT:
         *
         * First request:
         *     after = null
         *
         * Subsequent request:
         *     after = previous end_cursor
         */

        if (afterCursor == null ||
                afterCursor.isBlank()) {

            variables.putNull("after");

        } else {

            variables.put(
                    "after",
                    afterCursor
            );
        }

        variables.putNull("before");

        variables.put(
                "first",
                config.first()
        );

        variables.putNull("last");

        variables.putNull(
                "newer_than_message_id"
        );

        variables.putNull(
                "older_than_message_id"
        );

        variables.put(
                "id",
                config.conversationId()
        );

        variables.put(
                "__relay_internal__pv__IGDInitialMessagePageCountrelayprovider",
                config.first()
        );

        return variables.toString();
    }

    public String buildFormBody(
            String variables) {

        Map<String, String> form =
                new LinkedHashMap<>();

        form.put("av", "");

        form.put("__d", "www");
        form.put("__user", "0");
        form.put("__a", "1");

        form.put(
                "fb_api_caller_class",
                config.fbApiCallerClass()
        );

        form.put(
                "fb_api_req_friendly_name",
                config.friendlyName()
        );

        form.put(
                "server_timestamps",
                "true"
        );

        form.put(
                "variables",
                variables
        );

        form.put(
                "doc_id",
                config.docId()
        );

        form.put(
                "fb_dtsg",
                config.fbDtsg()
        );

        form.put(
                "jazoest",
                config.jazoest()
        );

        form.put(
                "lsd",
                config.lsd()
        );

        form.put("dpr", "3");

        form.put("__comet_req", "7");

        form.put(
                "__crn",
                "comet.igweb.PolarisDirectInboxMobileRoute"
        );

        return encodeForm(form);
    }

    private String encodeForm(
            Map<String, String> form) {

        StringBuilder result =
                new StringBuilder();

        for (Map.Entry<String, String> entry :
                form.entrySet()) {

            if (result.length() > 0) {
                result.append("&");
            }

            result
                    .append(urlEncode(entry.getKey()))
                    .append("=")
                    .append(urlEncode(entry.getValue()));
        }

        return result.toString();
    }

    private String urlEncode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}