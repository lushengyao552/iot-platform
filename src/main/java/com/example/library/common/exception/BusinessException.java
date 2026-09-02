package com.example.library.common.exception;

import com.example.library.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 *
 * <p>用于在业务逻辑中抛出可预期的异常，由全局异常处理器统一捕获并返回标准响应。
 * 与系统异常（如 NullPointerException）不同，业务异常是正常业务流程中的错误情况。
 *
 * <p>使用示例：
 * <pre>
 * if (user == null) {
 *     throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
 * }
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误状态码 */
    private final Integer code;

    /**
     * 使用状态码枚举构造异常
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 使用自定义状态码和消息构造异常
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用自定义消息构造异常（默认错误码 50000）
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }
}
