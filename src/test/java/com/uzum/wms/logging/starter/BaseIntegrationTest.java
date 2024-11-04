package com.uzum.wms.logging.starter;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
    @BeforeEach
    public final void baseInit() {
        WireMock.resetAllRequests();
    }

}
