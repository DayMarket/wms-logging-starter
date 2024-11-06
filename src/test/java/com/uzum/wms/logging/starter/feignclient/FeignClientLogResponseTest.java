package com.uzum.wms.logging.starter.feignclient;

import feign.Logger;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class FeignClientLogResponseTest {

    @Test
    void logAndRebufferResponseTest() {
        String url = "http://localhost:8080/api/test";
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("Header1", List.of("HeaderValue1"));
        byte[] bodyBytes = "{body: value}".getBytes();

        FeignClientCustomLogger feignClientCustomLogger = new FeignClientCustomLogger(100);

        Response sourceResponse = Response.builder()
                .request(
                        Request.create(Request.HttpMethod.POST,
                                url,
                                headers,
                                bodyBytes,
                                StandardCharsets.UTF_8,
                                null
                        )
                )
                .headers(headers)
                .body(bodyBytes)
                .build();
        Response rebufferResponse = feignClientCustomLogger.logAndRebufferResponse("FeignClient#getInfo(long)", Logger.Level.FULL, sourceResponse, 100);

        assertNotEquals(rebufferResponse, sourceResponse);
        assertEquals(url, rebufferResponse.request().url());
    }

    @Test
    void payloadOverLimitedTest(CapturedOutput capturedOutput) {
        String url = "http://localhost:8080/api/test";
        byte[] bodyBytes = "{body: value}".getBytes();

        FeignClientCustomLogger feignClientCustomLogger = new FeignClientCustomLogger(5);
        Response sourceResponse = Response.builder()
                .request(
                        Request.create(Request.HttpMethod.POST,
                                url,
                                Map.of(),
                                bodyBytes,
                                StandardCharsets.UTF_8,
                                null
                        )
                )
                .body(bodyBytes)
                .build();
        feignClientCustomLogger.logAndRebufferResponse("FeignClient#getInfo(long)", Logger.Level.FULL, sourceResponse, 100);

        assertTrue(capturedOutput.getOut().contains("response body content length is 13"));
    }
}
