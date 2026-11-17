package com.recommendation.common.exception;

import com.recommendation.common.enums.ResultCode;

/**
 * 业务异常
 */
public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(ResultCode.BUSINESS_ERROR.getCode(), message);
    }

    public BusinessException(Integer code, String message) {
        super(code, message);
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode);
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }
}
