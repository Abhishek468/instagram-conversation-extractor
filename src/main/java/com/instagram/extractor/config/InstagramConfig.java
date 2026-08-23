package com.instagram.extractor.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class InstagramConfig {

    private final Properties properties = new Properties();

    public InstagramConfig() throws IOException {

        Path configPath =
                Path.of("config", "instagram.properties");

        if (!Files.exists(configPath)) {
            throw new IllegalStateException(
                    "Missing config file: " + configPath +
                    "\nCopy instagram.properties.example to " +
                    "instagram.properties and configure it."
            );
        }

        try (InputStream input =
                     Files.newInputStream(configPath)) {

            properties.load(input);
        }
    }

    private String get(String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing configuration: " + key
            );
        }

        return value;
    }

    private String getOptional(String key) {
        return properties.getProperty(key, "");
    }

    public String baseUrl() {
        return get("instagram.base-url");
    }

    public String conversationId() {
        return get("instagram.conversation-id");
    }

    public int first() {
        return Integer.parseInt(
                get("instagram.first")
        );
    }

    public String docId() {
        return get("instagram.doc-id");
    }

    public String igAppId() {
        return get("instagram.ig-app-id");
    }

    public String friendlyName() {
        return get("instagram.friendly-name");
    }

    public String fbApiCallerClass() {
        return get("instagram.fb-api-caller-class");
    }

    public String xAsbdId() {
        return get("instagram.x-asbd-id");
    }

    public String userAgent() {
        return get("instagram.user-agent");
    }

    public String referer() {
        return get("instagram.referer");
    }

    public String csrfToken() {
        return get("instagram.csrf-token");
    }

    public String lsd() {
        return get("instagram.lsd");
    }

    public String fbDtsg() {
        return get("instagram.fb-dtsg");
    }

    public String jazoest() {
        return get("instagram.jazoest");
    }

    public String datr() {
        return get("instagram.datr");
    }

    public String igDid() {
        return get("instagram.ig-did");
    }

    public String mid() {
        return get("instagram.mid");
    }

    public String dsUserId() {
        return get("instagram.ds-user-id");
    }

    public String sessionId() {
        return get("instagram.sessionid");
    }

    public String rur() {
        return get("instagram.rur");
    }

    public int maxPages() {
        return Integer.parseInt(
                getOptional("extractor.max-pages")
                        .isBlank()
                        ? "4"
                        : getOptional("extractor.max-pages")
        );
    }

    public Path outputDirectory() {
        return Path.of(
                getOptional("extractor.output-directory")
                        .isBlank()
                        ? "data/conversations"
                        : getOptional("extractor.output-directory")
        );
    }
}