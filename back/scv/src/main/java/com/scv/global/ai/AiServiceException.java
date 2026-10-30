package com.scv.global.ai;

import com.scv.global.error.ErrorCode;
import com.scv.global.error.ServiceException;

public class AiServiceException extends ServiceException {
    public AiServiceException(ErrorCode code, Throwable cause) {
        super(code, cause);
    }

    public static AiServiceException invalidResponse() {
        return new AiServiceException(ErrorCode.AI_INVALID_RESPONSE, null);
    }
}
