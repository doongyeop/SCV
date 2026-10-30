package com.scv.global.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class AiClientConfig {
    @Bean
    AiTrainClient aiTrainClient(RestClient.Builder builder,
            @Value("${spring.fastapi.train.base-url}") String baseUrl,
            @Value("${spring.fastapi.connect-timeout:5s}") Duration connectTimeout,
            @Value("${spring.fastapi.train.read-timeout:30m}") Duration readTimeout) {
        return client(builder, baseUrl, connectTimeout, readTimeout, AiTrainClient.class);
    }

    @Bean
    AiAnalysisClient aiAnalysisClient(RestClient.Builder builder,
            @Value("${spring.fastapi.test.base-url}") String baseUrl,
            @Value("${spring.fastapi.connect-timeout:5s}") Duration connectTimeout,
            @Value("${spring.fastapi.test.read-timeout:5m}") Duration readTimeout) {
        return client(builder, baseUrl, connectTimeout, readTimeout, AiAnalysisClient.class);
    }

    @Bean
    AiSearchClient aiSearchClient(RestClient.Builder builder,
            @Value("${spring.fastapi.search.base-url}") String baseUrl,
            @Value("${spring.fastapi.connect-timeout:5s}") Duration connectTimeout,
            @Value("${spring.fastapi.search.read-timeout:10s}") Duration readTimeout) {
        return client(builder, baseUrl, connectTimeout, readTimeout, AiSearchClient.class);
    }

    private <T> T client(RestClient.Builder builder, String baseUrl, Duration connectTimeout,
                         Duration readTimeout, Class<T> type) {
        if (connectTimeout.isNegative() || connectTimeout.isZero()
                || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("AI timeouts must be positive");
        }
        var httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        var restClient = builder.clone().baseUrl(baseUrl).requestFactory(factory)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build().createClient(type);
    }
}
