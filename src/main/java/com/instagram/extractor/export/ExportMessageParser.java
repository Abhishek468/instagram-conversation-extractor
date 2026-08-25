package com.instagram.extractor.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagram.extractor.model.Attachment;
import com.instagram.extractor.model.Message;
import com.instagram.extractor.model.Participant;
import com.instagram.extractor.model.Reaction;
import com.instagram.extractor.model.SharedContent;

import java.util.ArrayList;
import java.util.List;

public class ExportMessageParser {

    private final ObjectMapper objectMapper;

    public ExportMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Message> parse(
            JsonNode root,
            String sourceFileName) {

        JsonNode messagesNode =
                root.path("messages");

        if (!messagesNode.isArray()) {
            throw new IllegalArgumentException(
                    "messages array not found in "
                            + sourceFileName
            );
        }

        List<Message> messages =
                new ArrayList<>();

        for (int i = 0;
             i < messagesNode.size();
             i++) {

            JsonNode node =
                    messagesNode.get(i);

            messages.add(
                    parseMessage(
                            node,
                            sourceFileName,
                            i
                    )
            );
        }

        return messages;
    }

    private Message parseMessage(
            JsonNode node,
            String sourceFileName,
            int messageIndex) {

        String id =
                buildMessageId(
                        sourceFileName,
                        messageIndex
                );

        String senderName =
                node.path("sender_name")
                        .asText(null);

        Participant sender =
                senderName == null
                        ? null
                        : new Participant(senderName);

        long timestampMs =
                node.path("timestamp_ms")
                        .asLong(0);

        String content =
                node.has("content")
                        ? node.path("content")
                                .asText(null)
                        : null;

        List<Reaction> reactions =
                parseReactions(
                        node.path("reactions")
                );

        SharedContent sharedContent =
                parseSharedContent(
                        node.path("share")
                );

        List<Attachment> attachments =
                parseAttachments(node);

        return new Message(
                id,
                timestampMs,
                sender,
                content,
                reactions,
                sharedContent,
                attachments,
                node.deepCopy()
        );
    }

    private List<Reaction> parseReactions(
            JsonNode reactionsNode) {

        if (!reactionsNode.isArray()) {
            return List.of();
        }

        List<Reaction> reactions =
                new ArrayList<>();

        for (JsonNode reactionNode :
                reactionsNode) {

            String reaction =
                    reactionNode.path("reaction")
                            .asText(null);

            String actorName =
                    reactionNode.path("actor")
                            .asText(null);

            Participant actor =
                    actorName == null
                            ? null
                            : new Participant(actorName);

            reactions.add(
                    new Reaction(
                            reaction,
                            actor
                    )
            );
        }

        return reactions;
    }

    private SharedContent parseSharedContent(
            JsonNode shareNode) {

        if (!shareNode.isObject()) {
            return null;
        }

        String link =
                shareNode.path("link")
                        .asText(null);

        String shareText =
                shareNode.path("share_text")
                        .asText(null);

        String owner =
                shareNode.path(
                                "original_content_owner"
                        )
                        .asText(null);

        return new SharedContent(
                link,
                shareText,
                owner
        );
    }

    private List<Attachment> parseAttachments(
            JsonNode messageNode) {

        List<Attachment> attachments =
                new ArrayList<>();

        parseMediaArray(
                messageNode.path("photos"),
                Attachment.Type.PHOTO,
                attachments
        );

        parseMediaArray(
                messageNode.path("videos"),
                Attachment.Type.VIDEO,
                attachments
        );

        parseMediaArray(
                messageNode.path("audio_files"),
                Attachment.Type.AUDIO,
                attachments
        );

        return attachments;
    }

    private void parseMediaArray(
            JsonNode mediaNode,
            Attachment.Type type,
            List<Attachment> attachments) {

        if (!mediaNode.isArray()) {
            return;
        }

        for (JsonNode media :
                mediaNode) {

            String uri =
                    media.path("uri")
                            .asText(null);

            if (uri == null || uri.isBlank()) {
                continue;
            }

            Long creationTimestamp =
                    media.has("creation_timestamp")
                            ? media.path(
                                    "creation_timestamp"
                            ).asLong()
                            : null;

            attachments.add(
                    new Attachment(
                            type,
                            uri,
                            creationTimestamp
                    )
            );
        }
    }

    private String buildMessageId(
            String sourceFileName,
            int messageIndex) {

        return "export:"
                + sourceFileName
                + ":"
                + messageIndex;
    }
}