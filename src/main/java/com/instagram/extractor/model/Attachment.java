package com.instagram.extractor.model;

public record Attachment(
        Type type,
        String relativePath,
        Long creationTimestamp
) {

    public enum Type {
        PHOTO,
        VIDEO,
        AUDIO
    }
}