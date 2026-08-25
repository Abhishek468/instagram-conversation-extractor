package com.instagram.extractor.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagram.extractor.model.Conversation;
import com.instagram.extractor.model.Message;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExportConversationReaderTest {

//     @Test
    void readsConversationDirectory() throws Exception {

        Path conversationDirectory =
                Path.of(
                        "src",
                        "test",
                        "resources",
                        "export",
                        "conversation"
                );

        ExportConversationReader reader =
                new ExportConversationReader(
                        new ObjectMapper()
                );

        Conversation conversation =
                reader.read(
                        conversationDirectory
                );

        assertNotNull(conversation);

        assertNotNull(
                conversation.id()
        );

        assertFalse(
                conversation.participants()
                        .isEmpty()
        );

        assertFalse(
                conversation.messages()
                        .isEmpty()
        );

        for (int i = 1;
             i < conversation.messages().size();
             i++) {

            Message previous =
                    conversation.messages()
                            .get(i - 1);

            Message current =
                    conversation.messages()
                            .get(i);

            assertTrue(
                    previous.timestampMs()
                            <= current.timestampMs(),
                    "Messages are not chronological"
            );
        }

        System.out.println();
        System.out.println(
                "========================================"
        );
        System.out.println(
                "EXPORT CONVERSATION READER TEST"
        );
        System.out.println(
                "========================================"
        );

        System.out.println(
                "Conversation: "
                        + conversation.id()
        );

        System.out.println(
                "Participants: "
                        + conversation.participants()
                                .size()
        );

        System.out.println(
                "Messages: "
                        + conversation.messages()
                                .size()
        );

        System.out.println(
                "First timestamp: "
                        + conversation.messages()
                                .get(0)
                                .timestampMs()
        );

        System.out.println(
                "Last timestamp: "
                        + conversation.messages()
                                .get(conversation.messages()
                                        .size() - 1)
                                .timestampMs()
        );

        System.out.println(
                "========================================"
        );
    }
}