// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE.md file distributed with this work for terms.

package com.yahoo.parsec.clients;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yahoo.parsec.test.ClientTestUtils;
import com.yahoo.parsec.test.WireMockBaseTest;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;

/**
 * Created by baiyi on 10/26/2018.
 */
public class ParsecAsyncHttpClientProfileLoggingTest extends WireMockBaseTest {

    private static ObjectMapper _OBJECT_MAPPER = new ObjectMapper();
    private static  ParsecAsyncHttpClient parsecHttpClient =
            new ParsecAsyncHttpClient.Builder().setAcceptAnyCertificate(true).build();



    Appender mockAppender = mock(Appender.class);
    String reqBodyJson, respBodyJson;
    ClientTestUtils testUtils = new ClientTestUtils();

    @BeforeMethod
    public void setup() throws JsonProcessingException {
        Logger logger = (Logger) LoggerFactory.getLogger("parsec.clients.profiling_log");
        mockAppender = mock(Appender.class);
        logger.addAppender(mockAppender);

        Map stubRequest = new HashMap<>();
        stubRequest.put("requestKey1", "requestValue1");
        stubRequest.put("requestKey2", "requestValue2");
        reqBodyJson = _OBJECT_MAPPER.writeValueAsString(stubRequest);

        Map stubResponse = new HashMap<>();
        stubResponse.put("respKey1", "respValue1");
        stubResponse.put("respKey2", "respValue2");
        respBodyJson = _OBJECT_MAPPER.writeValueAsString(stubResponse);

    }


    @Test
    public void nonCriticalGetShouldBeLogged() throws URISyntaxException, ExecutionException, InterruptedException {

        String url = "/get200profilelogging";
        WireMock.stubFor(get(urlEqualTo(url))
                .willReturn(okJson(respBodyJson)));


        String requestMethod = HttpMethod.GET;
        ParsecAsyncHttpRequest request = testUtils.buildRequest(requestMethod,300,
                new URI(super.wireMockBaseUrl+url), new HashMap<>(), reqBodyJson);

        Response response = parsecHttpClient.execute(request).get();

        assertThat(response.getStatus(), equalTo(200));

        then(mockAppender).should().doAppend(argThat(hasToString(containsString(url))));
    }

    @Test
    public void criticalPostRequestShouldBeLogged() throws URISyntaxException, ExecutionException, InterruptedException {

        String url = "/post200profilelogging";

        WireMock.stubFor(post(urlEqualTo(url))
                .withRequestBody(equalToJson(reqBodyJson))
                .willReturn(okJson(respBodyJson)));

        String requestMethod = HttpMethod.POST;
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("key1", Arrays.asList("headerValue"));

        ParsecAsyncHttpRequest request = testUtils.buildRequest(requestMethod,300,
                new URI(super.wireMockBaseUrl+url), headers, reqBodyJson);
        Response response = parsecHttpClient.criticalExecute(request).get();

        then(mockAppender).should().doAppend(argThat(hasToString(containsString(url))));

    }
}