package com.instagram.extractor.storage;

import java.time.OffsetDateTime;

public class ExtractionState {

    private String conversationId;
    private int totalMessages;

    private MessageBoundary newestMessage;
    private MessageBoundary oldestMessage;

    private SyncInfo lastSync;

    public ExtractionState() {
    }

    public ExtractionState(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(int totalMessages) {
        this.totalMessages = totalMessages;
    }

    public MessageBoundary getNewestMessage() {
        return newestMessage;
    }

    public void setNewestMessage(MessageBoundary newestMessage) {
        this.newestMessage = newestMessage;
    }

    public MessageBoundary getOldestMessage() {
        return oldestMessage;
    }

    public void setOldestMessage(MessageBoundary oldestMessage) {
        this.oldestMessage = oldestMessage;
    }

    public SyncInfo getLastSync() {
        return lastSync;
    }

    public void setLastSync(SyncInfo lastSync) {
        this.lastSync = lastSync;
    }

    public record MessageBoundary(
            String id,
            long timestampMs
    ) {
    }

    public record SyncInfo(
            String syncId,
            OffsetDateTime completedAt,
            String status
    ) {
    }
}