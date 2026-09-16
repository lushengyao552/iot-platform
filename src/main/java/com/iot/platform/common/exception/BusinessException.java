package com.iot.platform.common.exception;

import com.iot.platform.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
