package com.instagram.extractor.model;

import com.fasterxml.jackson.databind.JsonNode;

public record Message(
        String id,
        long timestampMs,
        JsonNode rawData
) {
}