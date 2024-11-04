package com.uzum.wms.logging.starter.listeners;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Slf4j
public class AsyncLoggingListener implements AsyncListener {
    private final ContentCachingResponseWrapper responseWrapper;

    public AsyncLoggingListener(ContentCachingResponseWrapper responseWrapper) {
        this.responseWrapper = responseWrapper;
    }

    public void onComplete(AsyncEvent asyncEvent) throws IOException {
        responseWrapper.copyBodyToResponse();
    }

    public void onTimeout(AsyncEvent asyncEvent) {
        //do nothing
    }

    public void onError(AsyncEvent asyncEvent) {
        log.error("Async request was complete with error:", asyncEvent.getThrowable());
    }

    public void onStartAsync(AsyncEvent asyncEvent) {
        //do nothing
    }
}
