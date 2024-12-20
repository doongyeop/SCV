package com.scv.domain.version.controller;

import com.scv.domain.data.enums.DataSet;
import com.scv.domain.model.dto.response.ModelCreateResponse;
import com.scv.global.oauth2.auth.AuthUser;
import com.scv.global.oauth2.auth.CustomOAuth2User;
import com.scv.domain.result.dto.response.ResultResponse;
import com.scv.domain.result.dto.response.ResultResponseWithImages;
import com.scv.domain.version.dto.request.ModelVersionRequest;
import com.scv.domain.version.dto.response.ModelVersionDetail;
import com.scv.domain.version.dto.response.ModelVersionOnWorking;
import com.scv.domain.version.service.ModelVersionService;
import com.scv.global.error.ErrorResponse;
import com.scv.global.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/models/versions")
@Tag(name = "모델 버전 컨트롤러", description = "모델 버전 관련 API")
@RequiredArgsConstructor
public class ModelVersionController {

    private final ModelVersionService modelVersionService;
    private final PageableUtil pageableUtil;

    // 모델 버전 생성
    @PostMapping("/{modelId}")
    @Operation(summary = "모델버전 생성", description = "모델 버전을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "모델버전 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 모델", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ModelCreateResponse> createModelVersion(@PathVariable Long modelId, @RequestParam Long modelVersionId, @AuthUser CustomOAuth2User user) throws BadRequestException {
        ModelCreateResponse modelResponse = modelVersionService.createModelVersion(modelId, modelVersionId, user);

        return ResponseEntity.status(201).body(modelResponse);
    }

    // 모델 버전 상세 조회
    @GetMapping("/public/{versionId}")
    @Operation(summary = "모델버전 상세조회", description = "모델 버전을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "모델버전 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    public ResponseEntity<ModelVersionDetail> getModelVersion(@PathVariable Long versionId) {
        ModelVersionDetail modelVersion = modelVersionService.getModelVersion(versionId);

        return ResponseEntity.ok(modelVersion);
    }

    // 모델 버전 수정
    @PatchMapping("/{versionId}")
    @Operation(summary = "모델버전  수정", description = "모델 버전을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "모델버전 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 모델", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> updateModelVersion(@PathVariable Long versionId, @RequestBody ModelVersionRequest request, @AuthUser CustomOAuth2User user) throws BadRequestException {
        modelVersionService.updateModelVersion(versionId, request, user);
        return ResponseEntity.ok().build();
    }

    // 모델 버전 삭제
    @DeleteMapping("{versionId}")
    @Operation(summary = "모델버전 삭제", description = "모델 버전을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "모델버전 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 모델버전", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteModelVersion(@PathVariable Long versionId, @AuthUser CustomOAuth2User user) throws BadRequestException {
        modelVersionService.deleteModelVersion(versionId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/working")
    @Operation(summary = "작업중인 모델 버전 조회", description = "작업중인 모델 버전을 조회합니다. orderBy = createdAt or updatedAt, direction = asc or desc. 미입력시 정렬 안함.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 모델버전", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<ModelVersionOnWorking>> getModelVersionsOnWorking(@RequestParam(defaultValue = "") DataSet dataName,
                                                                                 @RequestParam(defaultValue = "") String modelName,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "12") int size,
                                                                                 @RequestParam(required = false) String orderBy,
                                                                                 @RequestParam(required = false) String direction,
                                                                                 @AuthUser CustomOAuth2User user) {

        Pageable pageable = pageableUtil.createPageable(page, size, orderBy, direction);

        Page<ModelVersionOnWorking> modelVersions = modelVersionService.getModelVersionsOnWorking(user, pageable, modelName, dataName);

        return ResponseEntity.ok(modelVersions);
    }


    @PostMapping("/{versionId}/result/run")
    @Operation(summary = "실행 저장", description = "실행 결과를 저장하고 반환합니다.")
    public ResponseEntity<ResultResponse> saveAnalysis(@PathVariable Long versionId) {
        ResultResponse resultResponse = modelVersionService.runResult(versionId);
        return ResponseEntity.ok(resultResponse);
    }


    @PostMapping("/{versionId}/result/save")
    @Operation(summary = "결과 저장", description = "학습 결과(이미지들)를 저장하고 반환합니다.")
    public ResponseEntity<ResultResponseWithImages> saveResult(@PathVariable Long versionId) {
        ResultResponseWithImages resultResponseWithImages = modelVersionService.saveResult(versionId);
        return ResponseEntity.ok(resultResponseWithImages);
    }


}
