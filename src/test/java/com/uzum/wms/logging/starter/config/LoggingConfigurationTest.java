package com.uzum.wms.logging.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import com.uzum.wms.logging.starter.auto.LoggingConfiguration;
import com.uzum.wms.logging.starter.filters.RequestResponseFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class LoggingConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConditionEvaluationReportLoggingListener())
            .withUserConfiguration(LoggingConfiguration.class)
            .withBean(ObjectMapper.class);

    @Test
    void shouldRegisterAllBeans() {
        contextRunner.withPropertyValues("logging.redis.enabled:true")
                .run(context -> assertAll(
                        () -> assertThat(context).hasSingleBean(RequestResponseFilter.class)
                ));
    }
}
