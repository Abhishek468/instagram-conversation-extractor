package com.instagram.extractor.model;

public record Attachment(
        Type type,
        String relativePath
) {

    public enum Type {
        PHOTO,
        VIDEO,
        AUDIO,
        FILE,
        OTHER
    }
}