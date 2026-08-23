package com.instagram.extractor.storage;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExtractionManifest {

    private String conversationId;

    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;

    private String status;
    private String stopReason;

    private int messagesPerPage;
    private int pagesDownloaded;

    private boolean hasMorePages;

    private String firstEndCursor;
    private String lastEndCursor;

    private final List<PageMetadata> pages =
            new ArrayList<>();

    public ExtractionManifest() {
    }

    public ExtractionManifest(
            String conversationId,
            OffsetDateTime startedAt,
            int messagesPerPage) {

        this.conversationId = conversationId;
        this.startedAt = startedAt;
        this.messagesPerPage = messagesPerPage;
        this.status = "RUNNING";
        this.hasMorePages = false;
    }

    public void addPage(PageMetadata page) {

        pages.add(page);

        pagesDownloaded = pages.size();

        if (firstEndCursor == null) {
            firstEndCursor =
                    page.returnedEndCursor();
        }

        lastEndCursor =
                page.returnedEndCursor();

        hasMorePages =
                page.hasNextPage();
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(
            String conversationId) {

        this.conversationId = conversationId;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(
            OffsetDateTime startedAt) {

        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(
            OffsetDateTime completedAt) {

        this.completedAt = completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public int getMessagesPerPage() {
        return messagesPerPage;
    }

    public void setMessagesPerPage(
            int messagesPerPage) {

        this.messagesPerPage = messagesPerPage;
    }

    public int getPagesDownloaded() {
        return pagesDownloaded;
    }

    public void setPagesDownloaded(
            int pagesDownloaded) {

        this.pagesDownloaded = pagesDownloaded;
    }

    public boolean isHasMorePages() {
        return hasMorePages;
    }

    public void setHasMorePages(
            boolean hasMorePages) {

        this.hasMorePages = hasMorePages;
    }

    public String getFirstEndCursor() {
        return firstEndCursor;
    }

    public void setFirstEndCursor(
            String firstEndCursor) {

        this.firstEndCursor = firstEndCursor;
    }

    public String getLastEndCursor() {
        return lastEndCursor;
    }

    public void setLastEndCursor(
            String lastEndCursor) {

        this.lastEndCursor = lastEndCursor;
    }

    public List<PageMetadata> getPages() {
        return pages;
    }

    public record PageMetadata(
            int pageNumber,
            String fileName,
            int nodeCount,
            String firstMessageId,
            String lastMessageId,
            String returnedEndCursor,
            boolean hasNextPage
    ) {
    }
}