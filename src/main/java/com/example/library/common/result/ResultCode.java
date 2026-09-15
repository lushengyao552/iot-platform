package com.example.library.common.result;

import lombok.Getter;

@Getter 
public enum ResultCode {
    SUCCESS(20000, "操作成功"),
    PARAM_ERROR(40000, "参数错误"),
    PARAM_NOT_NULL(40001, "参数不能为空"),
    PARAM_TYPE_ERROR(40002, "参数类型错误"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    TOKEN_INVALID(40101, "Token 无效"),
    TOKEN_EXPIRED(40102, "Token 已过期"),
    FORBIDDEN(40300, "没有权限访问"),
    NOT_FOUND(40400, "资源不存在"),
    USERNAME_EXIST(40010, "用户名已存在"),
    USERNAME_NOT_FOUND(40011, "用户不存在"),
    PASSWORD_ERROR(40012, "密码错误"),
    USER_DISABLED(40013, "账号已被禁用"),
    BOOK_NOT_FOUND(40020, "图书不存在"),
    BOOK_OUT_OF_STOCK(40021, "图书库存不足"),
    BOOK_ALREADY_BORROWED(40022, "您已借阅此书，尚未归还"),
    BORROW_LIMIT_EXCEEDED(40030, "借阅数量已达上限"),
    BORROW_RECORD_NOT_FOUND(40031, "借阅记录不存在"),
    BORROW_ALREADY_RETURNED(40032, "该书已归还"),
    CATEGORY_NOT_FOUND(40040, "分类不存在"),
    CATEGORY_NAME_EXIST(40041, "分类名称已存在"),
    ERROR(50000, "系统内部错误");
    
    private final Integer code;
    private final String message;
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
