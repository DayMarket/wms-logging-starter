package com.uzum.wms.logging.starter.listeners;

import com.uzum.wms.logging.starter.listeners.AsyncLoggingListener;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.ContentCachingResponseWrapper;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncLoggingListenerTest {
    private AsyncListener asyncListener;

    @Mock
    private ContentCachingResponseWrapper response;

    @Mock
    private AsyncEvent asyncEvent;

    @BeforeEach
    void init() {
        this.asyncListener = new AsyncLoggingListener(response);
    }

    @Test
    @SneakyThrows
    void onCompleteTest() {
        //mock
        doNothing().when(response).copyBodyToResponse();
        //test
        asyncListener.onComplete(asyncEvent);
        verify(response, times(1)).copyBodyToResponse();
    }

    @Test
    @SneakyThrows
    void onErrorTest() {
        //mock
        when(asyncEvent.getThrowable()).thenReturn(new Exception("some exception"));
        //test
        asyncListener.onError(asyncEvent);
        verify(asyncEvent, times(1)).getThrowable();
    }
}
