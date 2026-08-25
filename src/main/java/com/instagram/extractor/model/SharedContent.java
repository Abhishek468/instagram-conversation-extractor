package com.instagram.extractor.model;

public record SharedContent(
        String link,
        String shareText,
        String originalContentOwner
) {
}