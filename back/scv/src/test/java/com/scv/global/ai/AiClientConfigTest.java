package com.scv.global.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import com.scv.global.error.ErrorCode;

import java.net.InetSocketAddress;
import java.time.Duration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.*;

class AiClientConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> {
                var yaml = new YamlPropertiesFactoryBean();
                yaml.setResources(new ClassPathResource("application.yml"));
                context.getEnvironment().getPropertySources().addLast(
                        new PropertiesPropertySource("application-yaml", yaml.getObject()));
                context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance());
            })
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withUserConfiguration(AiClientConfig.class);

    @Test
    void existingHostAndPortSettingsStillRegisterAllClients() {
        contextRunner.withPropertyValues("FAST_MODEL_TRAIN_HOST_NAME=train.internal", "FAST_MODEL_TRAIN_PORT=8003",
                "FAST_MODEL_TEST_HOST_NAME=test.internal", "FAST_MODEL_TEST_PORT=8002")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(AiTrainClient.class)
                            .hasSingleBean(AiAnalysisClient.class).hasSingleBean(AiSearchClient.class);
                    assertThat(context.getEnvironment().getProperty("spring.fastapi.train.base-url"))
                            .isEqualTo("http://train.internal:8003");
                });
    }

    @Test
    void baseUrlOverridesWorkWithoutLegacyHostAndPort() {
        contextRunner.withPropertyValues("FAST_MODEL_TRAIN_BASE_URL=http://train.test",
                "FAST_MODEL_TEST_BASE_URL=http://analysis.test", "FAST_MODEL_SEARCH_BASE_URL=http://search.test")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(AiTrainClient.class)
                        .hasSingleBean(AiAnalysisClient.class).hasSingleBean(AiSearchClient.class));
    }

    @Test
    void configuredClientUsesCustomBaseUrlAndDeleteMethod() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fast/v1/model/match/1/2", exchange -> {
            exchange.sendResponseHeaders("DELETE".equals(exchange.getRequestMethod()) ? 204 : 405, -1);
            exchange.close();
        });
        server.start();
        try {
            var client = new AiClientConfig().aiSearchClient(RestClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(1), Duration.ofSeconds(1));
            assertThatCode(() -> client.delete(1L, 2L)).doesNotThrowAnyException();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void configuredReadTimeoutIsEnforced() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            exchange.close();
        });
        server.start();
        try {
            var client = new AiClientConfig().aiSearchClient(RestClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(1), Duration.ofMillis(50));
            var gateway = new AiModelGateway(null, null, client);
            assertThatThrownBy(() -> gateway.delete(1L, 2L)).isInstanceOfSatisfying(AiServiceException.class,
                    error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.AI_TIMEOUT));
        } finally {
            server.stop(0);
        }
    }
}
