package com.scv.global.ai;

import com.scv.domain.result.dto.request.ResultRequest;
import com.scv.global.ai.dto.TrainResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface AiTrainClient {
    @PostExchange("/fast/v1/models/{modelId}/versions/{versionId}")
    TrainResponse train(@PathVariable("modelId") Long modelId,
                        @PathVariable("versionId") Long versionId,
                        @RequestBody ResultRequest request);
}
