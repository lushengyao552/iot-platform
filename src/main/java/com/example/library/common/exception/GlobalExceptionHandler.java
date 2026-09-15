package com.example.library.common.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;
import com.example.library.common.result.Result;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindException;
import java.util.stream.Collectors;


// **文件**：`common/exception/GlobalExceptionHandler.java`

// ### 前置"为什么"
// > **为什么要全局异常处理？如果不写这个类，业务异常会返回什么给前端？**

// ### 代码要求

// **类注解**：`@RestControllerAdvice` `@Slf4j`

// | 异常类型 | HTTP状态 | 返回 | 日志 |
// |----------|---------|------|------|
// | BusinessException | 200 | Result.error(e.getCode(), e.getMessage()) | warn |
// | MethodArgumentNotValidException | 400 | 收集字段错误拼接 | warn |
// | BindException | 400 | 同上 | warn |
// | Exception（兜底） | 500 | "系统内部错误"，code=50000 | error |

// **取字段错误**：
// ```java
// String message = e.getBindingResult().getFieldErrors().stream()
//     .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
//     .collect(Collectors.joining("; "));
// ```

// ### 易错点检查
// - [ ] 有兜底的 `@ExceptionHandler(Exception.class)`
// - [ ] 每个方法返回 Result<Void>

// ### 三级提示
// - **L1**：一个方法处理一种异常类型
// - **L2**：`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`
// - **L3**：方法签名 `@ExceptionHandler(BusinessException.class) public Result<Void> handleBusiness(BusinessException e) { log.warn(...); return Result.error(e.getCode(), e.getMessage()); }`

// ---
@RestControllerAdvice
@Slf4j 
public class GlobalExceptionHandler {
    @ExceptionHandler (MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常: {}", message);
        return Result.error(40000, message);
    }
    @ExceptionHandler (BindException.class)
    public Result<Void> handleBind(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定异常: {}", message);
        return Result.error(40000, message);
    }
    @ExceptionHandler (Exception.class)
    public Result<Void> handleException(Exception e) {  
        log.error("系统异常: ", e);
        return Result.error(50000, "系统内部错误");
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
}
