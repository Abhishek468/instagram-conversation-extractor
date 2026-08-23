package com.instagram.extractor.instagram;

import com.instagram.extractor.config.InstagramConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class InstagramClient {

    private final InstagramConfig config;
    private final GraphQLRequestBuilder requestBuilder;
    private final HttpClient httpClient;

    public InstagramClient(
            InstagramConfig config,
            GraphQLRequestBuilder requestBuilder) {

        this.config = config;
        this.requestBuilder = requestBuilder;

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(30)
                        )
                        .build();
    }

    public String fetchPage(
            String afterCursor) throws Exception {

        String variables =
                requestBuilder.buildVariables(
                        afterCursor
                );

        String formBody =
                requestBuilder.buildFormBody(
                        variables
                );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        config.baseUrl()
                                                + "/api/graphql"
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(60)
                        )
                        .header(
                                "Accept",
                                "*/*"
                        )
                        .header(
                                "Content-Type",
                                "application/x-www-form-urlencoded"
                        )
                        .header(
                                "Origin",
                                config.baseUrl()
                        )
                        .header(
                                "Referer",
                                config.referer()
                        )
                        .header(
                                "User-Agent",
                                config.userAgent()
                        )
                        .header(
                                "X-ASBD-ID",
                                config.xAsbdId()
                        )
                        .header(
                                "X-CSRFToken",
                                config.csrfToken()
                        )
                        .header(
                                "X-FB-Friendly-Name",
                                config.friendlyName()
                        )
                        .header(
                                "X-FB-LSD",
                                config.lsd()
                        )
                        .header(
                                "X-IG-App-ID",
                                config.igAppId()
                        )
                        .header(
                                "Cookie",
                                buildCookieHeader()
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(formBody)
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {

            throw new IllegalStateException(
                    "Instagram returned HTTP " +
                    response.statusCode() +
                    "\nResponse: " +
                    response.body()
            );
        }

        return response.body();
    }

    private String buildCookieHeader() {

        return String.join(
                "; ",
                "datr=" + config.datr(),
                "ig_did=" + config.igDid(),
                "mid=" + config.mid(),
                "csrftoken=" + config.csrfToken(),
                "ds_user_id=" + config.dsUserId(),
                "sessionid=" + config.sessionId(),
                "rur=" + config.rur()
        );
    }
}