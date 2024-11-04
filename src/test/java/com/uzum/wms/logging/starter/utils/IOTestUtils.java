package com.uzum.wms.logging.starter.utils;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class IOTestUtils {

    private IOTestUtils() {
    }

    public static String getResourceAsString(String resourcePath) {
        try {
            return IOUtils.toString(Objects.requireNonNull(IOTestUtils.class.getResource(resourcePath)),
                    StandardCharsets.UTF_8);
        } catch(IOException exception) {
            return "";
        }
    }

    public static String getResourceAsStringWithPlaceholders(String resourcePath, Object... placeholders) {
        String content = getResourceAsString(resourcePath);
        for (Object placeholder : placeholders) {
            content = StringUtils.replaceOnce(content, "PLACEHOLDER", placeholder.toString());
        }
        return content;
    }
}
