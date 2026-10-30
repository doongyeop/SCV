package com.scv.global.ai;

import com.scv.domain.data.enums.DataSet;
import com.scv.domain.result.dto.request.ResultRequest;
import com.scv.global.ai.dto.AnalysisResponse;
import com.scv.global.ai.dto.TrainResponse;
import com.scv.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class AiModelGateway {
    private final AiTrainClient trainClient;
    private final AiAnalysisClient analysisClient;
    private final AiSearchClient searchClient;

    public TrainResponse.TrainingResult train(Long modelId, Long versionId, ResultRequest request) {
        TrainResponse response = call(() -> trainClient.train(modelId, versionId, request));
        if (response == null) throw AiServiceException.invalidResponse();
        return response.validatedResult(modelId, versionId);
    }

    public AnalysisResponse analyze(Long modelId, Long versionId, DataSet dataset) {
        String name = dataset == DataSet.Fashion ? "fashion_mnist" : dataset.name().toLowerCase(Locale.ROOT);
        AnalysisResponse response = call(() -> analysisClient.analyze(modelId, versionId, name));
        if (response == null) throw AiServiceException.invalidResponse();
        response.validate(modelId, versionId);
        return response;
    }

    public void delete(Long modelId, Long versionId) {
        call(() -> { searchClient.delete(modelId, versionId); return null; });
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException
                        || cause instanceof TimeoutException) {
                    throw new AiServiceException(ErrorCode.AI_TIMEOUT, e);
                }
            }
            throw new AiServiceException(ErrorCode.AI_UNAVAILABLE, e);
        } catch (RestClientResponseException e) {
            throw new AiServiceException(ErrorCode.AI_REQUEST_FAILED, e);
        } catch (RestClientException e) {
            throw new AiServiceException(ErrorCode.AI_INVALID_RESPONSE, e);
        }
    }
}
