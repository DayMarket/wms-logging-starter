package com.uzum.wms.logging.starter.feignclient;

import feign.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;


@ConditionalOnClass(FeignClient.class)
public class FeignClientCustomLoggerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Logger logger(@Value("${logging.maxPayload:5000}") Integer maxPayload) {
        return new FeignClientCustomLogger(maxPayload);
    }

}
