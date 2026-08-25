package com.instagram.extractor.export;

import com.instagram.extractor.model.Attachment;

import java.nio.file.Files;
import java.nio.file.Path;

public class MediaResolver {

    private final Path exportRoot;

    public MediaResolver(Path exportRoot) {

        if (exportRoot == null) {
            throw new IllegalArgumentException(
                    "exportRoot must not be null"
            );
        }

        this.exportRoot =
                exportRoot.toAbsolutePath()
                        .normalize();
    }

    public Path resolve(Attachment attachment) {

        if (attachment == null) {
            throw new IllegalArgumentException(
                    "attachment must not be null"
            );
        }

        String relativePath =
                attachment.relativePath();

        if (relativePath == null
                || relativePath.isBlank()) {

            throw new IllegalArgumentException(
                    "Attachment has no relative path"
            );
        }

        Path resolved =
                exportRoot
                        .resolve(relativePath)
                        .normalize();

        if (!resolved.startsWith(exportRoot)) {
            throw new SecurityException(
                    "Attachment path escapes export root: "
                            + relativePath
            );
        }

        return resolved;
    }

    public boolean exists(
            Attachment attachment) {

        return Files.isRegularFile(
                resolve(attachment)
        );
    }
}