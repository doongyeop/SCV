package com.scv.domain.version.dto.response;

import com.scv.domain.version.dto.layer.LayerDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ModelVersionDetail {
    private final Long modelVersionId;
    private final List<LayerDTO> layers;
}
