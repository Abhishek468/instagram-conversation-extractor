package com.instagram.extractor.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagram.extractor.model.Attachment;
import com.instagram.extractor.model.Message;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExportMessageParserTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

//     @Test
    void parsesInstagramExportMessages() throws Exception {

        InputStream input =
                getClass()
                        .getResourceAsStream(
                                "/export/message_1.json"
                        );

        assertNotNull(
                input,
                "Test export JSON not found"
        );

        JsonNode root =
                objectMapper.readTree(input);

        ExportMessageParser parser =
                new ExportMessageParser(
                        objectMapper
                );

        List<Message> messages =
                parser.parse(
                        root,
                        "message_1.json"
                );

        assertFalse(
                messages.isEmpty(),
                "Expected exported messages"
        );

        System.out.println();
        System.out.println(
                "========================================"
        );
        System.out.println(
                "EXPORT MESSAGE PARSER TEST"
        );
        System.out.println(
                "========================================"
        );

        System.out.println(
                "Messages parsed: "
                        + messages.size()
        );

        for (Message message : messages) {

            System.out.println(
                    "ID: "
                            + message.id()
            );

            System.out.println(
                    "Sender: "
                            + (
                            message.sender() == null
                                    ? "<none>"
                                    : message.sender().name()
                    )
            );

            System.out.println(
                    "Timestamp: "
                            + message.timestampMs()
            );

            System.out.println(
                    "Content: "
                            + message.content()
            );

            System.out.println(
                    "Reactions: "
                            + message.reactions().size()
            );

            System.out.println(
                    "Shared content: "
                            + (
                            message.sharedContent() != null
                    )
            );

            System.out.println(
                    "Attachments: "
                            + message.attachments().size()
            );

            for (Attachment attachment :
                    message.attachments()) {

                System.out.println(
                        "  - "
                                + attachment.type()
                                + ": "
                                + attachment.relativePath()
                );
            }

            System.out.println(
                    "----------------------------------------"
            );
        }

        System.out.println(
                "========================================"
        );
    }
}