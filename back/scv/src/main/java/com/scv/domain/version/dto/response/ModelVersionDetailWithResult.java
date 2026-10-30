package com.scv.domain.version.dto.response;

import com.scv.domain.result.dto.response.ResultResponseWithImages;
import com.scv.domain.version.dto.layer.LayerDTO;
import java.util.List;
import lombok.Getter;

@Getter
public class ModelVersionDetailWithResult extends ModelVersionDetail {
    private final ResultResponseWithImages resultResponseWithImages;

    public ModelVersionDetailWithResult(Long versionId, List<LayerDTO> layers, ResultResponseWithImages resultResponseWithImages) {
        super(versionId, layers);
        this.resultResponseWithImages = resultResponseWithImages;
    }

}
