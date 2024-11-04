package com.uzum.wms.logging.starter.auto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzum.wms.logging.starter.filters.RequestResponseFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class LoggingConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestResponseFilter requestResponseFilter(ObjectMapper objectMapper,
                                                       @Value("${logging.maxPayload:5000}") Integer maxPayload) {
        return new RequestResponseFilter(objectMapper, maxPayload);
    }

}
