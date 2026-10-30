package com.scv.global.ai;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.DeleteExchange;

public interface AiSearchClient {
    @DeleteExchange("/fast/v1/model/match/{modelId}/{versionId}")
    void delete(@PathVariable("modelId") Long modelId,
                @PathVariable("versionId") Long versionId);
}
