package com.uzum.wms.logging.starter.constants;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum LoggingAttribute {

    HTTP_METHOD("httpMethod"),
    HTTP_URI("uri"),
    HTTP_HEADERS("headers"),
    REMOTE_ADDRESS("remoteAddress"),
    HTTP_PAYLOAD("payload"),
    ;

    private final String name;

}
