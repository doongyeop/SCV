package com.scv.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.scv.global.ai.AiServiceException;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TrainResponse(String status, Long modelId, Long versionId,
                            @JsonProperty("test_results") TestResults testResults) {
    public TrainingResult validatedResult(Long expectedModelId, Long expectedVersionId) {
        if (!"success".equals(status) || !expectedModelId.equals(modelId)
                || !expectedVersionId.equals(versionId) || testResults == null
                || testResults.results() == null) {
            throw AiServiceException.invalidResponse();
        }
        testResults.results().validate();
        return testResults.results();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TestResults(TrainingResult results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TrainingResult(
            @JsonProperty("final_test_accuracy") Double accuracy,
            @JsonProperty("final_test_loss") Double loss,
            @JsonProperty("model_code") String modelCode,
            @JsonProperty("layer_parameters") List<Integer> layerParameters,
            @JsonProperty("train_result_per_epoch") JsonNode resultPerEpoch,
            @JsonProperty("training_history") JsonNode trainingHistory) {
        public void validate() {
            if (accuracy == null || !Double.isFinite(accuracy) || loss == null || !Double.isFinite(loss)
                    || modelCode == null || modelCode.isBlank() || layerParameters == null
                    || layerParameters.stream().anyMatch(value -> value == null || value < 0)
                    || resultPerEpoch == null || !resultPerEpoch.isObject()
                    || trainingHistory == null || !trainingHistory.isArray()
                    || layerParameters.stream().mapToLong(Integer::longValue).sum() > Integer.MAX_VALUE) {
                throw AiServiceException.invalidResponse();
            }
        }

        public int totalParams() {
            return layerParameters.stream().mapToInt(Integer::intValue).sum();
        }
    }
}
