package com.scv.domain.version.dto.request;

import com.scv.domain.version.dto.layer.LayerDTO;

import java.util.List;

public record ModelVersionRequest(
        int versionNo,
        List<LayerDTO> layers
) {
}