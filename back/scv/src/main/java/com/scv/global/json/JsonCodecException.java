package com.scv.global.json;

import com.scv.global.error.ErrorCode;
import com.scv.global.error.ServiceException;

public class JsonCodecException extends ServiceException {
    public JsonCodecException(Throwable cause) {
        super(ErrorCode.JSON_PROCESSING_ERROR, cause);
    }
}
