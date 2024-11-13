package com.uzum.wms.logging.starter.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import com.uzum.wms.logging.starter.feignclient.FeignClientCustomLoggerConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class FeignClientCustomLoggerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConditionEvaluationReportLoggingListener())
            .withUserConfiguration(FeignClientCustomLoggerConfiguration.class);

    @Test
    void shouldRegisterAllBeans() {
        contextRunner
                .run(context -> assertAll(
                        () -> assertThat(context).hasBean("logger"),
                        () -> assertThat(context).hasSingleBean(feign.Logger.class)
                ));
    }

}
