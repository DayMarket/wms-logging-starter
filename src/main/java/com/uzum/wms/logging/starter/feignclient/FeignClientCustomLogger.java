package com.uzum.wms.logging.starter.feignclient;

import feign.Logger;
import feign.Request;
import feign.Response;
import feign.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
public class FeignClientCustomLogger extends Logger {
    private final Integer maxPayload;

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        this.log(configKey, "Request URI: %s, Method: %s, Headers: %s, Request body: %s",
                request.url(),
                request.httpMethod().name(),
                request.headers(),
                request.body() != null ? new String(request.body(), StandardCharsets.UTF_8) : "empty-body"
        );
    }

    @Override
    public Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime) {
        String responsePayload = "";
        Response clonedResponse = response.toBuilder().build();

        if (Objects.nonNull(response.body())) {
            try {
                byte[] bodyData = Util.toByteArray(response.body().asInputStream());
                Response.Body responseBodyCopy = response.toBuilder().body(bodyData).build().body();
                clonedResponse = response.toBuilder().body(responseBodyCopy).build();
                responsePayload = buildResponsePayload(responseBodyCopy, bodyData.length);
            } catch (IOException exception) {
                log.error(
                        "Error while reading the Feign client response || Error Details: {}",
                        exception.getMessage());
            }
        }
        this.log(configKey, "Response took [%sms] Status code: %s, Headers: %s, Response body: %s",
                elapsedTime,
                response.status(),
                response.headers(),
                responsePayload
        );
        return clonedResponse;
    }

    @Override
    protected void log(String configKey, String format, Object... args) {
        log.info(format(configKey, format, args));
    }

    protected String format(String configKey, String format, Object... args) {
        String methodTag = methodTag(configKey);
        String formattedString = String.format(format, args);
        return methodTag + formattedString;
    }

    private String buildResponsePayload(Response.Body response, long contentLength) throws IOException {
        return contentLength > maxPayload ?
                String.format("Can't log payload: response body content length is %s", contentLength)
                : getResponseBody(response);
    }

    protected String getResponseBody(Response.Body response) throws IOException {
        return StreamUtils.copyToString(response.asInputStream(), Charset.defaultCharset());
    }
}
