package com.instagram.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.instagram.extractor.config.InstagramConfig;
import com.instagram.extractor.extraction.ConversationExtractor;
import com.instagram.extractor.instagram.GraphQLRequestBuilder;
import com.instagram.extractor.instagram.InstagramClient;
import com.instagram.extractor.instagram.InstagramResponseParser;
import com.instagram.extractor.storage.RawResponseStore;

public class Main {

    public static void main(String[] args) {

        try {

            InstagramConfig config =
                    new InstagramConfig();

            ObjectMapper objectMapper =
                    new ObjectMapper();

            objectMapper.registerModule(
                    new JavaTimeModule()
            );

            GraphQLRequestBuilder requestBuilder =
                    new GraphQLRequestBuilder(
                            config,
                            objectMapper
                    );

            InstagramClient client =
                    new InstagramClient(
                            config,
                            requestBuilder
                    );

            InstagramResponseParser parser =
                    new InstagramResponseParser(
                            objectMapper
                    );


            RawResponseStore store =
                    new RawResponseStore(
                            config.outputDirectory(),
                            config.conversationId(),
                            objectMapper
                    );
            ConversationExtractor extractor =
                    new ConversationExtractor(
                            config,
                            client,
                            parser,
                            store
                    );

            extractor.extract();

        } catch (Exception e) {

            System.err.println();
            System.err.println(
                    "EXTRACTION FAILED"
            );

            e.printStackTrace();

            System.exit(1);
        }
    }
}