package com.scv.domain;

import com.scv.domain.data.domain.Data;
import com.scv.domain.data.enums.DataSet;
import com.scv.domain.data.repository.DataRepository;
import com.scv.domain.model.domain.Model;
import com.scv.domain.model.dto.response.ModelDetailResponse;
import com.scv.domain.model.exception.ModelNotFoundException;
import com.scv.domain.model.repository.ModelRepository;
import com.scv.domain.model.service.ModelService;
import com.scv.domain.result.domain.Result;
import com.scv.domain.result.repository.ResultRepository;
import com.scv.domain.user.domain.User;
import com.scv.domain.user.repository.UserRepository;
import com.scv.domain.version.domain.ModelVersion;
import com.scv.domain.version.dto.request.ModelVersionRequest;
import com.scv.domain.version.exception.ModelVersionNotFoundException;
import com.scv.domain.version.repository.ModelVersionRepository;
import com.scv.domain.version.service.ModelVersionService;
import com.scv.global.oauth2.auth.CustomOAuth2User;
import com.scv.global.util.UrlUtil;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModelCorrectnessTest {
    private final ModelRepository models = mock(ModelRepository.class);
    private final ModelVersionRepository versions = mock(ModelVersionRepository.class);
    private final ResultRepository results = mock(ResultRepository.class);
    private final DataRepository data = mock(DataRepository.class);
    private final ModelService modelService = new ModelService(models, versions, data, mock(UserRepository.class));
    private final ModelVersionService versionService = new ModelVersionService(models, versions, results, data, mock(UrlUtil.class));
    private Model model;

    @BeforeEach
    void setUp() {
        model = Model.builder().id(1L).name("original")
                .user(User.builder().userId(1000L).build())
                .data(Data.builder().name(DataSet.MNIST).build())
                .latestVersion(3).accuracy(0.9).build();
    }

    private CustomOAuth2User actor(long id) {
        CustomOAuth2User user = mock(CustomOAuth2User.class);
        when(user.getUserId()).thenReturn(Long.valueOf(id));
        return user;
    }

    private ModelVersion version(long id, int number, boolean working, Result result) {
        return ModelVersion.builder().id(id).model(model).versionNo(number)
                .isWorkingOn(working).layers("[]").result(result).build();
    }

    @Test
    void ownerCanRenameModel() throws Exception {
        when(models.findById(1L)).thenReturn(Optional.of(model));
        modelService.updateModelName(1L, "renamed", actor(1000));
        assertThat(model.getName()).isEqualTo("renamed");
    }

    @Test
    void anotherUserCannotRenameModel() {
        when(models.findById(1L)).thenReturn(Optional.of(model));
        assertThatThrownBy(() -> modelService.updateModelName(1L, "renamed", actor(2000)))
                .isInstanceOf(BadRequestException.class);
        assertThat(model.getName()).isEqualTo("original");
        verify(models, never()).save(any());
    }

    @Test
    void ownerCanCopyAndEditVersionWithEqualNonCachedIds() throws Exception {
        ModelVersion source = version(10L, 1, false, null);
        when(models.findById(1L)).thenReturn(Optional.of(model));
        when(versions.findById(10L)).thenReturn(Optional.of(source));
        CustomOAuth2User owner = actor(1000);
        assertThat(owner.getUserId()).isEqualTo(model.getUser().getUserId())
                .isNotSameAs(model.getUser().getUserId());

        versionService.createModelVersion(1L, 10L, owner);
        versionService.updateModelVersion(10L, new ModelVersionRequest(List.of()), owner);

        verify(versions).save(argThat(v -> v.getVersionNo() == 0 && v.getModel() == model));
        verify(results).findById(10L);
    }

    @Test
    void anotherUserCannotCopyOrEditVersion() {
        ModelVersion source = version(10L, 1, false, null);
        when(models.findById(1L)).thenReturn(Optional.of(model));
        when(versions.findById(10L)).thenReturn(Optional.of(source));
        CustomOAuth2User stranger = actor(2000);
        assertThatThrownBy(() -> versionService.createModelVersion(1L, 10L, stranger))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> versionService.updateModelVersion(10L, new ModelVersionRequest(List.of()), stranger))
                .isInstanceOf(BadRequestException.class);
        verify(versions, never()).save(any());
        verifyNoInteractions(results);
    }

    @Test
    void modelDetailExcludesDeletedVersionsAndKeepsOrder() {
        ModelVersion deleted = version(30L, 3, false, null);
        deleted.delete();
        model = model.toBuilder().modelVersions(List.of(version(10L, 1, false, null), deleted,
                version(20L, 2, false, null))).build();
        assertThat(new ModelDetailResponse(model).modelVersions())
                .extracting(v -> v.versionId()).containsExactly(20L, 10L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deletedVersionOrParentCannotBeReadModifiedOrExecuted(boolean deleteParent) {
        ModelVersion target = version(10L, 1, false, null);
        if (deleteParent) model.delete(); else target.delete();
        when(versions.findById(10L)).thenReturn(Optional.of(target));
        CustomOAuth2User owner = actor(1000);

        assertThatThrownBy(() -> versionService.getModelVersion(10L)).isInstanceOf(ModelVersionNotFoundException.class);
        assertThatThrownBy(() -> versionService.updateModelVersion(10L, new ModelVersionRequest(List.of()), owner))
                .isInstanceOf(ModelVersionNotFoundException.class);
        assertThatThrownBy(() -> versionService.deleteModelVersion(10L, owner)).isInstanceOf(ModelVersionNotFoundException.class);
        assertThatThrownBy(() -> versionService.runResult(10L)).isInstanceOf(ModelVersionNotFoundException.class);
        assertThatThrownBy(() -> versionService.saveResult(10L)).isInstanceOf(ModelVersionNotFoundException.class);
        verifyNoInteractions(results, data);
    }

    @Test
    void deletedModelCannotBeReadOrRenamed() {
        model.delete();
        when(models.findById(1L)).thenReturn(Optional.of(model));
        assertThatThrownBy(() -> modelService.getModelVersions(1L)).isInstanceOf(ModelNotFoundException.class);
        assertThatThrownBy(() -> modelService.updateModelName(1L, "renamed", actor(1000)))
                .isInstanceOf(ModelNotFoundException.class);
    }

    @Test
    void activeVersionWithoutActiveResultStillReturnsLayers() {
        when(versions.findById(10L)).thenReturn(Optional.of(version(10L, 1, false, null)));
        when(results.findByIdAndDeletedFalse(10L)).thenReturn(Optional.empty());
        assertThat(versionService.getModelVersion(10L).getLayers()).isEmpty();
        verify(results, never()).findById(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"empty", "draft", "missing-result", "deleted-result"})
    void deletingLastCompletedVersionResetsSummary(String remainingType) throws Exception {
        Result inactive = Result.builder().testAccuracy(0.8).build();
        inactive.delete();
        List<ModelVersion> remaining = switch (remainingType) {
            case "draft" -> List.of(version(20L, 0, true, Result.builder().testAccuracy(0.7).build()));
            case "missing-result" -> List.of(version(20L, 2, false, null));
            case "deleted-result" -> List.of(version(20L, 2, false, inactive));
            default -> List.of();
        };
        deleteLatestWithRemaining(remaining);
        assertThat(model.getLatestVersion()).isZero();
        assertThat(model.getAccuracy()).isEqualTo(-1.0);
    }

    @Test
    void deletingLatestSelectsHighestRemainingCompletedVersion() throws Exception {
        deleteLatestWithRemaining(List.of(
                version(10L, 1, false, Result.builder().testAccuracy(0.95).build()),
                version(20L, 2, false, Result.builder().testAccuracy(0.8).build()),
                version(40L, 0, true, Result.builder().testAccuracy(0.99).build())));
        assertThat(model.getLatestVersion()).isEqualTo(2);
        assertThat(model.getAccuracy()).isEqualTo(0.8);
    }

    private void deleteLatestWithRemaining(List<ModelVersion> remaining) throws Exception {
        Result result = Result.builder().testAccuracy(0.9).build();
        ModelVersion target = version(30L, 3, false, result);
        when(versions.findById(30L)).thenReturn(Optional.of(target));
        when(results.findByIdAndDeletedFalse(30L)).thenReturn(Optional.of(result));
        when(versions.findAllByModelIdAndDeletedFalse(1L)).thenReturn(remaining);
        // Keep PR 1 independent of the real AI server; client extraction belongs to PR 2.
        try (var clients = mockConstruction(RestTemplate.class)) {
            versionService.deleteModelVersion(30L, actor(1000));
            assertThat(clients.constructed()).hasSize(1);
        }
        assertThat(target.isDeleted()).isTrue();
        assertThat(result.isDeleted()).isTrue();
    }
}
