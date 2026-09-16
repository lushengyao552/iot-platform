package com.iot.platform.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(20000, "操作成功"),
    PARAM_ERROR(40000, "参数错误"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    TOKEN_INVALID(40101, "Token 无效"),
    FORBIDDEN(40300, "没有权限"),
    NOT_FOUND(40400, "资源不存在"),
    USERNAME_EXIST(40010, "用户名已存在"),
    USER_NOT_FOUND(40011, "用户不存在"),
    PASSWORD_ERROR(40012, "密码错误"),
    PRODUCT_NOT_FOUND(40020, "产品不存在"),
    PRODUCT_KEY_EXIST(40021, "产品Key已存在"),
    DEVICE_NOT_FOUND(40030, "设备不存在"),
    DEVICE_OFFLINE(40031, "设备离线"),
    DEVICE_SECRET_ERROR(40032, "设备密钥错误"),
    ERROR(50000, "系统内部错误");

    private final Integer code;
    private final String message;
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
