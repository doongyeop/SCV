package com.scv.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.scv.global.ai.AiServiceException;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisResponse(
        @JsonProperty("model_version_id") String modelVersionId,
        @JsonProperty("confusion_matrix") String confusionMatrix,
        @JsonProperty("example_image") String exampleImage,
        @JsonProperty("feature_activation") JsonNode featureActivation,
        @JsonProperty("activation_maximization") JsonNode activationMaximization) {
    public void validate(Long modelId, Long versionId) {
        if (!("model_" + modelId + "_v" + versionId).equals(modelVersionId)
                || confusionMatrix == null || exampleImage == null
                || featureActivation == null || !featureActivation.isArray()
                || activationMaximization == null || !activationMaximization.isArray()) {
            throw AiServiceException.invalidResponse();
        }
    }
}
