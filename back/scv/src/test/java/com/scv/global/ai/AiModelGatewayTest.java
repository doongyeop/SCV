package com.scv.global.ai;

import com.scv.domain.data.enums.DataSet;
import com.scv.domain.result.dto.request.ResultRequest;
import com.scv.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiModelGatewayTest {
    private static final String TRAIN = """
            {"status":"success","modelId":1,"versionId":2,"test_results":{"results":{
            "model_code":"print('model')","final_test_accuracy":0.9,"final_test_loss":0.1,
            "layer_parameters":[10,20],"train_result_per_epoch":{"accuracy":[0.9],"loss":[0.1]},
            "training_history":[],"model_layer":[]}}}
            """;
    private static final String ANALYSIS = """
            {"model_version_id":"model_1_v2","confusion_matrix":"matrix-base64",
             "example_image":"image-base64","feature_activation":[],"activation_maximization":[],
             "dataset":"fashion_mnist"}
            """;
    private MockRestServiceServer server;
    private AiModelGateway gateway;
    private final ResultRequest request = new ResultRequest(new ResultRequest.ModelLayerConfig(List.of()),
            "MNIST", 10, 5, 10, 1);

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai.test");
        server = MockRestServiceServer.bindTo(builder).build();
        var proxy = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build())).build();
        gateway = new AiModelGateway(proxy.createClient(AiTrainClient.class),
                proxy.createClient(AiAnalysisClient.class), proxy.createClient(AiSearchClient.class));
    }

    @Test
    void sendsTrainingContractAndReadsNestedResult() {
        server.expect(requestTo("http://ai.test/fast/v1/models/1/versions/2"))
                .andExpect(method(HttpMethod.POST)).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.modelLayerAt.layers").isArray())
                .andExpect(jsonPath("$.dataName").value("MNIST"))
                .andExpect(jsonPath("$.dataEpochCnt").value(1))
                .andRespond(withSuccess(TRAIN, MediaType.APPLICATION_JSON));
        var result = gateway.train(1L, 2L, request);
        assertThat(result.accuracy()).isEqualTo(0.9);
        assertThat(result.totalParams()).isEqualTo(30);
        assertThat(result.modelCode()).isEqualTo("print('model')");
        server.verify();
    }

    @Test
    void mapsFashionDatasetAndReadsAnalysis() {
        server.expect(requestTo("http://ai.test/fast/v1/model/test/analyze/1/2/fashion_mnist"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess(ANALYSIS, MediaType.APPLICATION_JSON));
        assertThat(gateway.analyze(1L, 2L, DataSet.Fashion).confusionMatrix()).isEqualTo("matrix-base64");
        server.verify();
    }

    @Test
    void deletesVectorUsingSearchEndpoint() {
        server.expect(requestTo("http://ai.test/fast/v1/model/match/1/2"))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withNoContent());
        gateway.delete(1L, 2L);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"final_test_accuracy", "final_test_loss", "model_code", "layer_parameters",
            "train_result_per_epoch", "training_history", "test_results"})
    void rejectsMissingRequiredTrainingFields(String field) {
        expectTraining(TRAIN.replace('"' + field + '"', '"' + "unknown_" + field + '"'));
        assertError(() -> gateway.train(1L, 2L, request), ErrorCode.AI_INVALID_RESPONSE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "null", "not-json", "{\"status\":\"failed\"}"})
    void rejectsEmptyOrMalformedResponses(String response) {
        expectTraining(response);
        assertError(() -> gateway.train(1L, 2L, request), ErrorCode.AI_INVALID_RESPONSE);
    }

    @Test
    void rejectsResultForAnotherVersion() {
        expectTraining(TRAIN.replace("\"versionId\":2", "\"versionId\":3"));
        assertError(() -> gateway.train(1L, 2L, request), ErrorCode.AI_INVALID_RESPONSE);
    }

    @Test
    void zeroAccuracyIsValid() {
        expectTraining(TRAIN.replace("\"final_test_accuracy\":0.9", "\"final_test_accuracy\":0.0"));
        assertThat(gateway.train(1L, 2L, request).accuracy()).isZero();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"confusion_matrix", "example_image", "feature_activation", "activation_maximization"})
    void rejectsIncompleteAnalysis(String field) {
        server.expect(anything()).andRespond(withSuccess(
                ANALYSIS.replace('"' + field + '"', '"' + "unknown_" + field + '"'), MediaType.APPLICATION_JSON));
        assertError(() -> gateway.analyze(1L, 2L, DataSet.MNIST), ErrorCode.AI_INVALID_RESPONSE);
    }

    @ParameterizedTest
    @ValueSource(ints = {422, 500, 503})
    void translatesHttpFailuresWithoutRetry(int status) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status)));
        assertError(() -> gateway.train(1L, 2L, request), ErrorCode.AI_REQUEST_FAILED);
    }

    @Test
    void distinguishesTimeoutFromConnectionFailure() {
        server.expect(anything()).andRespond(withException(new SocketTimeoutException("timeout")));
        assertError(() -> gateway.train(1L, 2L, request), ErrorCode.AI_TIMEOUT);
    }

    @Test
    void translatesConnectionFailure() {
        server.expect(anything()).andRespond(withException(new IOException("connection refused")));
        assertError(() -> gateway.train(1L, 2L, request), ErrorCode.AI_UNAVAILABLE);
    }

    private void expectTraining(String response) {
        server.expect(anything()).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, ErrorCode code) {
        assertThatThrownBy(action).isInstanceOfSatisfying(AiServiceException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(code));
        server.verify();
    }
}
