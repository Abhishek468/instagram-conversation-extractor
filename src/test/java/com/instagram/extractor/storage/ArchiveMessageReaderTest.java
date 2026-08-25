package com.instagram.extractor.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagram.extractor.instagram.InstagramResponseParser;
import com.instagram.extractor.model.Message;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveMessageReaderTest {

//     @Test
    void shouldRetrieveMessageUsingIndexedLocation()
            throws Exception {

        Path conversationDirectory =
                Path.of(
                        "data",
                        "conversations",
                        "1796143968442156"
                );

        ObjectMapper objectMapper =
                new ObjectMapper()
                        .findAndRegisterModules();

        InstagramResponseParser parser =
                new InstagramResponseParser(
                        objectMapper
                );

        MessageIndex messageIndex =
                new JsonMessageIndex(
                        conversationDirectory,
                        objectMapper,
                        parser
                );

        ArchiveMessageReader reader =
                new ArchiveMessageReader(
                        conversationDirectory,
                        objectMapper,
                        parser,
                        messageIndex
                );

        String messageId =
                "mid.$cAD8IUtXoBLumYC7ZuWgLumNEw6Xf";

        /*
         * 1. Verify the index knows the message.
         */
        assertTrue(
                messageIndex.contains(messageId),
                "Message should exist in the index"
        );

        /*
         * 2. Verify the index has a physical location.
         */
        Optional<MessageLocation> location =
                messageIndex.getLocation(
                        messageId
                );

        assertTrue(
                location.isPresent(),
                "Message should have an indexed location"
        );

        MessageLocation messageLocation =
                location.get();

        System.out.println();
        System.out.println(
                "========================================"
        );
        System.out.println(
                "INDEXED MESSAGE LOOKUP TEST"
        );
        System.out.println(
                "========================================"
        );
        System.out.println(
                "Message ID: " + messageId
        );
        System.out.println(
                "Sync ID: " +
                        messageLocation.syncId()
        );
        System.out.println(
                "Page: " +
                        messageLocation.page()
        );

        /*
         * 3. Ask the data layer for the message.
         */
        Optional<Message> result =
                reader.getMessage(
                        messageId
                );

        /*
         * 4. Verify that the message was actually found.
         */
        assertTrue(
                result.isPresent(),
                "Indexed message should be found"
        );

        /*
         * 5. Verify that the returned message is
         * actually the requested message.
         */
        assertEquals(
                messageId,
                result.get().id()
        );

        System.out.println(
                "Lookup result: FOUND"
        );
        System.out.println(
                "Timestamp: " +
                        result.get().timestampMs()
        );
        System.out.println(
                "========================================"
        );
    }
}