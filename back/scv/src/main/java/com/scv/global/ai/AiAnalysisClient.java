package com.scv.global.ai;

import com.scv.global.ai.dto.AnalysisResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface AiAnalysisClient {
    @GetExchange("/fast/v1/model/test/analyze/{modelId}/{versionId}/{dataset}")
    AnalysisResponse analyze(@PathVariable("modelId") Long modelId,
                             @PathVariable("versionId") Long versionId,
                             @PathVariable("dataset") String dataset);
}
