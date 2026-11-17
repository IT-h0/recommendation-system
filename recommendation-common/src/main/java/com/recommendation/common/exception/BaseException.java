package com.recommendation.common.exception;

import com.recommendation.common.enums.ResultCode;
import lombok.Getter;

/**
 * 基础异常
 */
@Getter
public class BaseException extends RuntimeException {

    private final Integer code;
    private final String message;

    public BaseException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
        this.message = message;
    }

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BaseException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public BaseException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }
}
