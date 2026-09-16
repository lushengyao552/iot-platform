package com.example.library.common.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return new Result<>(20000, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(20000, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(20000, message, data);
    }

    public static <T> Result<T> error() {
        return new Result<>(50000, "系统内部错误", null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(50000, message, null);
    }

    public static <T> Result<T> error(ResultCode rc) {
        return new Result<>(rc.getCode(), rc.getMessage(), null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return code != null && code == 20000;
    }
}
