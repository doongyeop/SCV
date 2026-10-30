package com.scv.global.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scv.domain.data.domain.Data;
import com.scv.domain.data.enums.DataSet;
import com.scv.domain.data.repository.DataRepository;
import com.scv.domain.model.domain.Model;
import com.scv.domain.model.repository.ModelRepository;
import com.scv.domain.result.domain.Result;
import com.scv.domain.result.repository.ResultRepository;
import com.scv.domain.version.domain.ModelVersion;
import com.scv.domain.version.repository.ModelVersionRepository;
import com.scv.domain.version.service.ModelVersionService;
import com.scv.global.ai.dto.AnalysisResponse;
import com.scv.global.ai.dto.TrainResponse;
import com.scv.global.json.JsonCodec;
import com.scv.global.json.JsonCodecException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModelAiPersistenceTest {
    private final ModelRepository models = mock(ModelRepository.class);
    private final ModelVersionRepository versions = mock(ModelVersionRepository.class);
    private final ResultRepository results = mock(ResultRepository.class);
    private final DataRepository datasets = mock(DataRepository.class);
    private final AiModelGateway gateway = mock(AiModelGateway.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ModelVersionService service = new ModelVersionService(models, versions, results, datasets, gateway, new JsonCodec(mapper));
    private final Data data = Data.builder().id(1).name(DataSet.MNIST).build();
    private final Model model = Model.builder().id(1L).data(data).latestVersion(0).build();
    private final ModelVersion version = ModelVersion.builder().id(2L).model(model).layers("[]").build();

    @Test
    void corruptStoredLayersNeverReachAiServer() {
        version.updateLayers("[] {}");
        when(versions.findById(2L)).thenReturn(Optional.of(version));
        when(datasets.findById(1)).thenReturn(Optional.of(data));
        assertThatThrownBy(() -> service.runResult(2L)).isInstanceOf(JsonCodecException.class);
        verifyNoInteractions(gateway, results);
    }

    @Test
    void detailReturnsParsedLayerList() {
        when(versions.findById(2L)).thenReturn(Optional.of(version));
        var detail = service.getModelVersion(2L);
        assertThat(detail.getModelVersionId()).isEqualTo(2L);
        assertThat(detail.getLayers()).isEmpty();
    }

    @Test
    void preservesTrainingJsonAndParameterCount() throws Exception {
        when(versions.findById(2L)).thenReturn(Optional.of(version));
        when(datasets.findById(1)).thenReturn(Optional.of(data));
        when(gateway.train(eq(1L), eq(2L), any())).thenReturn(new TrainResponse.TrainingResult(
                0.9, 0.1, "print('model')", List.of(10, 20), mapper.readTree("{\"accuracy\":[0.9]}"),
                mapper.readTree("[]")));
        var response = service.runResult(2L);
        assertThat(mapper.readTree(response.getCodeView()).asText()).isEqualTo("print('model')");
        assertThat(mapper.readTree(response.getLayerParams())).isEqualTo(mapper.readTree("[10,20]"));
        assertThat(mapper.readTree(response.getTrainInfos()).has("train_result_per_epoch")).isTrue();
        assertThat(response.getTotalParams()).isEqualTo(30);
        verify(results).save(any(Result.class));
    }

    @Test
    void invalidAiResponseDoesNotSaveResult() {
        when(versions.findById(2L)).thenReturn(Optional.of(version));
        when(datasets.findById(1)).thenReturn(Optional.of(data));
        when(gateway.train(eq(1L), eq(2L), any())).thenThrow(AiServiceException.invalidResponse());
        assertThatThrownBy(() -> service.runResult(2L)).isInstanceOf(AiServiceException.class);
        verifyNoInteractions(results);
    }

    @Test
    void preservesImageStringAndArrayStorageFormat() throws Exception {
        Result result = Result.builder().id(2L).modelVersion(version).testAccuracy(0.9).testLoss(0.1).build();
        when(versions.findById(2L)).thenReturn(Optional.of(version));
        when(results.findById(2L)).thenReturn(Optional.of(result));
        when(gateway.analyze(1L, 2L, DataSet.MNIST)).thenReturn(new AnalysisResponse(
                "model_1_v2", "matrix", "image", mapper.readTree("[]"), mapper.readTree("[]")));
        service.saveResult(2L);
        assertThat(mapper.readTree(result.getConfusionMatrix()).asText()).isEqualTo("matrix");
        assertThat(mapper.readTree(result.getExampleImg()).asText()).isEqualTo("image");
        assertThat(mapper.readTree(result.getFeatureActivation()).isArray()).isTrue();
        assertThat(model.getLatestVersion()).isEqualTo(1);
    }
}
