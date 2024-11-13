package com.uzum.wms.logging.starter.utils;

import lombok.experimental.UtilityClass;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@UtilityClass
public class IOTestUtils {

    public static String getResourceAsString(String resourcePath) {
        try {
            return IOUtils.toString(Objects.requireNonNull(IOTestUtils.class.getResource(resourcePath)),
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

}
