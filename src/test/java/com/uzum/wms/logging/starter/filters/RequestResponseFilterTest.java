package com.uzum.wms.logging.starter.filters;

import com.uzum.wms.logging.starter.BaseIntegrationTest;
import com.uzum.wms.logging.starter.auto.LoggingConfiguration;
import com.uzum.wms.logging.starter.utils.IOTestUtils;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;

import static com.uzum.wms.logging.starter.filters.RequestResponseFilter.INCOMING_REQUEST_TIME;
import static com.uzum.wms.logging.starter.filters.RequestResponseFilter.INIT_URI;
import static org.codehaus.plexus.util.ReaderFactory.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

@ExtendWith({OutputCaptureExtension.class})
@ContextConfiguration(classes = LoggingConfiguration.class)
class RequestResponseFilterTest extends BaseIntegrationTest {
    private static final String RESPONSE_CONTENT = "{\"response\":\"ok\"}";

    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/complete");

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final FilterChain filterChain = (request, response) -> response.getWriter().write(RESPONSE_CONTENT);

    @Autowired
    private RequestResponseFilter requestResponseFilter;

    @BeforeEach
    void init() {
        request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }


    @Test
    void afterRequestTest() {
        requestResponseFilter.afterRequest(request, "message");
        assertNull(MDC.get(INIT_URI));
    }

    @Test
    @SneakyThrows
    void doFilterInternalWithJsonContentTypeTest(CapturedOutput capturedOutput) {
        request.setQueryString("invoiceId=1");
        request.setContent("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MzA4MzU");

        response.setStatus(200);
        response.setContentLength(RESPONSE_CONTENT.length());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setCharacterEncoding(UTF_8);
        //test
        requestResponseFilter.doFilterInternal(request, response, filterChain);

        //check
        String outLog = capturedOutput.getOut();
        String actualRequestLog = outLog.substring(outLog.indexOf("{\"requestInfo"), outLog.indexOf("}}}") + 3);
        String actualResponseLog = outLog.substring(outLog.indexOf("{\"responseInfo"), outLog.length() - 1);

        String expectedRequestLog = IOTestUtils.getResourceAsString("/logging/RequestInfoLog.json");
        assertEquals(expectedRequestLog, actualRequestLog, new CustomComparator(JSONCompareMode.STRICT,
                Customization.customization("requestInfo.remoteAddress",
                        (expectedRemoteAddress, actualRemoteAddress)
                                -> StringUtils.isNotBlank((String) actualRemoteAddress)),

                Customization.customization("requestInfo.uri", (expectedUri, actualUri)
                        -> StringUtils.contains((String) actualUri, "/complete?invoiceId=1"))));

        String expectedResponseLog =
                IOTestUtils.getResourceAsString("/logging/ResponseInfoLogWithContentTypeJson.json");
        assertEquals(expectedResponseLog, actualResponseLog, new CustomComparator(JSONCompareMode.STRICT,
                Customization.customization("responseInfo.uri", (expectedUri, actualUri)
                        -> StringUtils.contains((String) actualUri, "/complete?invoiceId=1"))));
    }

    @Test
    @SneakyThrows
    void doFilterInternalWithoutContentTypeTest(CapturedOutput capturedOutput) {
        String expectedRequestLog = IOTestUtils.getResourceAsString("/logging/RequestInfoLog.json");

        request.setQueryString("invoiceId=1");
        request.setContent("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setCharacterEncoding(UTF_8);

        response.setStatus(200);
        response.setContentLength(RESPONSE_CONTENT.length());
        //test
        requestResponseFilter.doFilterInternal(request, response, filterChain);

        //check
        String outLog = capturedOutput.getOut();
        String actualRequestLog = outLog.substring(outLog.indexOf("{\"requestInfo"), outLog.indexOf("}}}") + 3);
        String actualResponseLog = outLog.substring(outLog.indexOf("{\"responseInfo"), outLog.length() - 1);

        assertEquals(expectedRequestLog, actualRequestLog, new CustomComparator(JSONCompareMode.STRICT,
                Customization.customization("requestInfo.remoteAddress",
                        (expectedRemoteAddress, actualRemoteAddress)
                                -> StringUtils.isNotBlank((String) actualRemoteAddress)),

                Customization.customization("requestInfo.uri", (expectedUri, actualUri)
                        -> StringUtils.contains((String) actualUri, "/complete?invoiceId=1"))));

        String expectedResponseLog = IOTestUtils.getResourceAsString(
                "/logging/ResponseInfoLogWithoutContentTypeJson.json");
        assertEquals(expectedResponseLog, actualResponseLog, new CustomComparator(JSONCompareMode.STRICT,
                Customization.customization("responseInfo.uri", (expectedUri, actualUri)
                        -> StringUtils.contains((String) actualUri, "/complete?invoiceId=1"))));
    }

    @Test
    @SneakyThrows
    void doFilterInternalWithPdfContentTypeTest(CapturedOutput capturedOutput) {
        request.setQueryString("invoiceId=1");
        request.setContent("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setCharacterEncoding(UTF_8);

        response.setStatus(200);
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setContentLength(RESPONSE_CONTENT.length());
        //test
        requestResponseFilter.doFilterInternal(request, response,
                (request, response) -> response.getWriter().write(RESPONSE_CONTENT));

        //check
        String outLog = capturedOutput.getOut();
        String actualRequestLog = outLog.substring(outLog.indexOf("{\"requestInfo"), outLog.indexOf("}}}") + 3);
        String actualResponseLog = outLog.substring(outLog.indexOf("{\"responseInfo"), outLog.length() - 1);

        String expectedRequestLog = IOTestUtils.getResourceAsString("/logging/RequestInfoLog.json");
        assertEquals(expectedRequestLog, actualRequestLog, new CustomComparator(JSONCompareMode.STRICT,
                Customization.customization("requestInfo.remoteAddress",
                        (expectedRemoteAddress, actualRemoteAddress)
                                -> StringUtils.isNotBlank((String) actualRemoteAddress)),

                Customization.customization("requestInfo.uri", (expectedUri, actualUri)
                        -> StringUtils.contains((String) actualUri, "/complete?invoiceId=1"))));

        String expectedResponseLog = IOTestUtils.getResourceAsString(
                "/logging/ResponseInfoLogWithContentTypePDF.json");
        assertEquals(expectedResponseLog, actualResponseLog, new CustomComparator(JSONCompareMode.STRICT,
                Customization.customization("responseInfo.uri", (expectedUri, actualUri)
                        -> StringUtils.contains((String) actualUri, "/complete?invoiceId=1"))));
    }

    @Test
    @SneakyThrows
    void doFilterInternalMaxPayloadExceedTest(CapturedOutput capturedOutput) {
        String contentMaxPayloadExceed = "Lorem ipsum dolor sit amet, consectetuer adipiscin";

        request.setQueryString("invoiceId=1");
        request.setContent("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setCharacterEncoding(UTF_8);

        response.setStatus(200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setContentLength(contentMaxPayloadExceed.length());
        //test
        requestResponseFilter.doFilterInternal(request, response,
                (request, response) -> response.getWriter().write(contentMaxPayloadExceed));

        //check
        String outLog = capturedOutput.getOut();
        String actualRequestLog = outLog.substring(outLog.indexOf("{\"requestInfo"), outLog.indexOf("}}}") + 3);
        String actualResponseLog = outLog.substring(outLog.indexOf("{\"responseInfo"), outLog.length() - 1);

        String expectedRequestLog = IOTestUtils.getResourceAsString("/logging/RequestInfoLog.json");
        assertEquals(expectedRequestLog, actualRequestLog, new CustomComparator(JSONCompareMode.STRICT,
                Customization.customization("requestInfo.remoteAddress",
                        (expectedRemoteAddress, actualRemoteAddress)
                                -> StringUtils.isNotBlank((String) actualRemoteAddress)),

                Customization.customization("requestInfo.uri", (expectedUri, actualUri)
                        -> StringUtils.contains((String) actualUri, "/complete?invoiceId=1"))));

        String expectedResponseLog = IOTestUtils.getResourceAsString(
                "/logging/ResponseInfoLogMaxPayloadExceed.json");
        assertEquals(expectedResponseLog, actualResponseLog, new CustomComparator(JSONCompareMode.STRICT,
                Customization.customization("responseInfo.uri", (expectedUri, actualUri)
                        -> StringUtils.contains((String) actualUri, "/complete?invoiceId=1"))));
    }

    @Test
    @SneakyThrows
    void doFilterInternalWithArbitraryContentTypeTest(CapturedOutput capturedOutput) {
        request.setQueryString("invoiceId=1");
        request.setContent("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setCharacterEncoding(UTF_8);

        response.setStatus(200);
        response.setContentType(MediaType.TEXT_XML_VALUE);
        response.setContentLength(RESPONSE_CONTENT.length());
        //test
        requestResponseFilter.doFilterInternal(request, response,
                (request, response) -> response.getWriter().write("<response>"));

        //check
        String outLog = capturedOutput.getOut();
        String actualRequestLog = outLog.substring(outLog.indexOf("{\"requestInfo"), outLog.indexOf("}}}") + 3);
        String actualResponseLog = outLog.substring(outLog.indexOf("{\"responseInfo"), outLog.length() - 1);

        String expectedRequestLog = IOTestUtils.getResourceAsString("/logging/RequestInfoLog.json");
        assertEquals(expectedRequestLog, actualRequestLog, new CustomComparator(JSONCompareMode.STRICT,
                Customization.customization("requestInfo.remoteAddress",
                        (expectedRemoteAddress, actualRemoteAddress)
                                -> StringUtils.isNotBlank((String) actualRemoteAddress)),

                Customization.customization("requestInfo.uri", (expectedUri, actualUri)
                        -> StringUtils.contains((String) actualUri, "/complete?invoiceId=1"))));

        String expectedResponseLog = IOTestUtils.getResourceAsString(
                "/logging/ResponseInfoLogWithArbitraryContentType.json");
        assertEquals(expectedResponseLog, actualResponseLog, new CustomComparator(JSONCompareMode.STRICT,
                Customization.customization("responseInfo.uri", (expectedUri, actualUri)
                        -> StringUtils.contains((String) actualUri, "/complete?invoiceId=1"))));
    }

    @SneakyThrows
    @Test
    void loggingWithoutActuatorTest(CapturedOutput capturedOutput) {
        MockHttpServletRequest actuatorRequest = new MockHttpServletRequest("GET", "/actuator/prometheus");
        actuatorRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.setStatus(200);
        response.setContentLength(RESPONSE_CONTENT.length());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        //test
        requestResponseFilter.doFilterInternal(actuatorRequest, response, filterChain);

        //check проверяем, что не логируется для url /actuator
        String outLog = capturedOutput.getOut();
        assertFalse(outLog.contains("requestInfo"));
        assertFalse(outLog.contains("responseInfo"));
    }

    @SneakyThrows
    @Test
    void beforeRequestEmptyAllHeadersTest() {
        //test
        requestResponseFilter.beforeRequest(request, StringUtils.EMPTY);
        //check
        assertNotNull(request.getAttribute(INCOMING_REQUEST_TIME));
    }

    @SneakyThrows
    @Test
    void whenRequestAsync_thenNotLogResponse() {
        RequestResponseFilter reqResFilter = spy(requestResponseFilter);
        request.setAsyncStarted(true);
        HttpServletRequest spyRequest = spy(request);
        when(spyRequest.getAsyncContext()).thenReturn(mock(AsyncContext.class));

        //test
        reqResFilter.doFilterInternal(spyRequest, response, filterChain);

        //check
        verify(spyRequest, times(1)).getAsyncContext();
        verify(spyRequest.getAsyncContext(), times(1)).addListener(any());
    }

    @Test
    void testDoFilterInternalForActuatorPrometheus() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/actuator/prometheus");

        // test
        requestResponseFilter.doFilterInternal(request, response, filterChain);

        //check
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain, times(1)).doFilter(requestCaptor.capture(), eq(response));
        assertInstanceOf(ContentCachingRequestWrapper.class, requestCaptor.getValue());
    }

    @SneakyThrows
    @Test
    void payloadMoreThenMaxPayloadTest(CapturedOutput capturedOutput) {
        String largeContent = RandomStringUtils.random(41);
        request.setContent(largeContent.getBytes(StandardCharsets.UTF_8));

        //test
        requestResponseFilter.doFilterInternal(request, response, filterChain);

        //check
        String outLog = capturedOutput.getOut();
        assertFalse(
                outLog.contains("java.lang.StringIndexOutOfBoundsException: Range [0, 40) out of bounds for length"));
        Assertions.assertTrue(
                outLog.contains("Payload length=41 but maxPayload=40. Please, increase logging.maxPayload"));
    }

    @SneakyThrows
    @Test
    void largeContentSizeButPayloadLessThenMaxPayloadTest(CapturedOutput capturedOutput) {
        String largeContent = RandomStringUtils.random(39);
        request.setContent(largeContent.getBytes(StandardCharsets.UTF_8));

        //test
        requestResponseFilter.doFilterInternal(request, response, filterChain);

        //check
        String outLog = capturedOutput.getOut();
        assertFalse(
                outLog.contains("java.lang.StringIndexOutOfBoundsException: Range [0, 40) out of bounds for length"));
        assertFalse(
                outLog.contains("Payload length=50 but maxPayload=40. Please, increase logging.maxPayload"));
    }

}
