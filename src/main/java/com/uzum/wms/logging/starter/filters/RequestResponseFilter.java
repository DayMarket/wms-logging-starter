package com.uzum.wms.logging.starter.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.uzum.wms.logging.starter.config.CachedBodyHttpServletRequest;
import com.uzum.wms.logging.starter.constants.LoggingAttribute;
import com.uzum.wms.logging.starter.listeners.AsyncLoggingListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_CBOR_VALUE;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.IMAGE_GIF_VALUE;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.MediaType.MULTIPART_MIXED_VALUE;
import static org.springframework.http.MediaType.MULTIPART_RELATED_VALUE;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@Slf4j
public class RequestResponseFilter extends AbstractRequestLoggingFilter {
    public static final String INCOMING_REQUEST_TIME = "INCOMING_REQUEST_TIME";
    private static final String DURATION = "duration";
    public static final String INIT_URI = "initUri";
    private static final String HTTP_STATUS = "httpStatus";
    private final ObjectMapper objectMapper;
    private final Integer maxPayload;

    private static final Set<String> EXCLUDED_HEADERS = Set.of("authorization");
    private static final List<String> EXCLUDED_URI =
            List.of("/actuator/prometheus", "/actuator/health", "/actuator/env", "/swagger-ui/**", "/swagger-ui.html",
                    "/or/v3/api-docs**", "/or/v3/api-docs/**", "/v3/api-docs/**", "/v3/api-docs**");

    public RequestResponseFilter(ObjectMapper objectMapper, Integer maxPayload) {
        this.setIncludePayload(true);
        this.setMaxPayloadLength(maxPayload);
        this.maxPayload = maxPayload;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        request.setAttribute(INCOMING_REQUEST_TIME, LocalDateTime.now());
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        //do nothing
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (containsActuatorUrls(request.getRequestURI())) {
            super.doFilterInternal(request, response, filterChain);
        } else {
            ContentCachingResponseWrapper cachingResponseWrapper = new ContentCachingResponseWrapper(response);
            CachedBodyHttpServletRequest cachedBodyHttpServletRequest = new CachedBodyHttpServletRequest(request);
            try {
                logRequest(cachedBodyHttpServletRequest);
                super.doFilterInternal(cachedBodyHttpServletRequest, cachingResponseWrapper, filterChain);
            } finally {
                logResponse(cachedBodyHttpServletRequest, cachingResponseWrapper);
                clearMDC();
            }
        }
    }

    private void clearMDC() {
        MDC.remove(INIT_URI);
        MDC.remove(DURATION);
        MDC.remove(HTTP_STATUS);
    }

    private void logRequest(HttpServletRequest httpServletRequest) {
        beforeRequest(httpServletRequest);
        try {
            ServletServerHttpRequest servletServerHttpRequest = new ServletServerHttpRequest(httpServletRequest);
            ObjectNode message = objectMapper.createObjectNode();
            message.putPOJO(LoggingAttribute.HTTP_URI.getName(), servletServerHttpRequest.getURI());
            message.put(LoggingAttribute.HTTP_METHOD.getName(), httpServletRequest.getMethod());
            message.putPOJO(LoggingAttribute.HTTP_HEADERS.getName(), getHeaders(servletServerHttpRequest));
            String remoteAddr = httpServletRequest.getRemoteAddr();
            if (StringUtils.isNotBlank(remoteAddr)) {
                message.put(LoggingAttribute.REMOTE_ADDRESS.getName(), remoteAddr);
            }
            JsonNode payload = buildPayload(httpServletRequest.getContentType(), httpServletRequest.getInputStream());
            if (payload != null) {
                message.set(LoggingAttribute.HTTP_PAYLOAD.getName(), payload);
            }
            log.debug(objectMapper.createObjectNode().set("requestInfo", message).toString());
        } catch (Exception e) {
            log.error("Unable to build request message. Error is: ", e);
        }
    }

    private HttpHeaders getHeaders(ServletServerHttpRequest servletServerHttpRequest) {
        HttpHeaders filteredHeaders = new HttpHeaders();
        servletServerHttpRequest.getHeaders().forEach((headerName, headerValues) -> {
            // TODO можно маскировать, а не просто скипать чувствительные данные
            if (!EXCLUDED_HEADERS.contains(headerName.toLowerCase())) {
                filteredHeaders.put(headerName, headerValues);
            }
        });
        return filteredHeaders;
    }

    private void beforeRequest(HttpServletRequest request) {
        String uriPath = request.getRequestURI() + Optional.ofNullable(request.getQueryString())
                .map(qString -> "?" + qString).orElse("");
        MDC.put(INIT_URI, uriPath);
    }

    private void logResponse(CachedBodyHttpServletRequest requestWrapper, ContentCachingResponseWrapper
            responseWrapper) throws IOException {
        boolean isAsyncStarted = requestWrapper.isAsyncStarted();
        Duration duration = Duration.between((LocalDateTime) requestWrapper.getAttribute(INCOMING_REQUEST_TIME),
                LocalDateTime.now());
        try (var ignored1 = MDC.putCloseable(DURATION, String.valueOf(duration.toMillis()));
             var ignore3 = MDC.putCloseable(HTTP_STATUS, String.valueOf(responseWrapper.getStatus()))) {

            if (!isAsyncStarted) {
                ObjectNode message = objectMapper.createObjectNode();
                message.putPOJO(LoggingAttribute.HTTP_URI.getName(),
                        new ServletServerHttpRequest(requestWrapper).getURI());
                message.put(HTTP_STATUS, responseWrapper.getStatus());
                HttpHeaders httpHeaders = buildHeaders(responseWrapper);
                if (!httpHeaders.isEmpty()) {
                    message.putPOJO(LoggingAttribute.HTTP_HEADERS.getName(), httpHeaders);
                }
                JsonNode payload =
                        buildPayload(responseWrapper.getContentType(), responseWrapper.getContentInputStream());
                if (payload != null) {
                    message.set(LoggingAttribute.HTTP_PAYLOAD.getName(), payload);
                }
                log.debug(objectMapper.createObjectNode().set("responseInfo", message).toString());
            }
        } catch (Exception e) {
            log.error("Unable to build response message. Error is: ", e);
        } finally {
            if (isAsyncStarted) {
                requestWrapper.getAsyncContext()
                        .addListener(new AsyncLoggingListener(responseWrapper));
            } else {
                responseWrapper.copyBodyToResponse();
            }
        }
    }

    private boolean containsActuatorUrls(String url) {
        return EXCLUDED_URI.stream().anyMatch(excludeUrl -> excludeUrl.equals(url));
    }

    private JsonNode buildPayload(String contentType, InputStream inputStream) throws IOException {
        String payload = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        if (StringUtils.isNotBlank(payload)) {
            int payloadLength = payload.length();
            if (payloadLength > maxPayload) {
                return TextNode.valueOf(String.format("Payload length=%s but maxPayload=%s. " +
                                "Please, increase logging.maxPayload. Truncated content: %s", payloadLength, maxPayload,
                        payload.substring(0, maxPayload)));
            } else {
                return buildByContentType(contentType, payload);
            }
        }
        return null;
    }

    private JsonNode buildByContentType(String contentType, String payload) throws JsonProcessingException {
        if (StringUtils.isBlank(contentType)) {
            return TextNode.valueOf(String.format("Content-Type is not specified: %s", payload));
        } else if (APPLICATION_JSON_VALUE.equals(contentType) || APPLICATION_JSON_UTF8_VALUE.equals(contentType)) {
            return objectMapper.readTree(payload);
        } else {
            return switch (contentType) {
                case APPLICATION_PDF_VALUE,
                        APPLICATION_OCTET_STREAM_VALUE,
                        MULTIPART_FORM_DATA_VALUE,
                        APPLICATION_CBOR_VALUE,
                        APPLICATION_FORM_URLENCODED_VALUE,
                        IMAGE_GIF_VALUE,
                        IMAGE_JPEG_VALUE,
                        IMAGE_PNG_VALUE,
                        MULTIPART_MIXED_VALUE,
                        TEXT_EVENT_STREAM_VALUE,
                        MULTIPART_RELATED_VALUE -> TextNode.valueOf(String.format("Content-Type is %s", contentType));
                default -> TextNode.valueOf(String.format("Content-Type is %s. Content: %s", contentType, payload));
            };
        }
    }

    private HttpHeaders buildHeaders(ContentCachingResponseWrapper responseWrapper) {
        HttpHeaders httpHeaders = new HttpHeaders();
        responseWrapper.getHeaderNames()
                .forEach(headerName -> httpHeaders.add(headerName, responseWrapper.getHeader(headerName)));
        return httpHeaders;
    }

}
