package com.instagram.extractor.model;

public record Reaction(
        String reaction,
        Participant actor
) {
}