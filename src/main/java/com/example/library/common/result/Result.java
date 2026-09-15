package com.example.library.common.result;
import java.io.Serializable;

// 代码要求
import lombok.Data;

// **字段**：`Integer code`、`String message`、`T data`、`Long timestamp`

// **类注解**：`@Data`，`implements Serializable`，加 `serialVersionUID`

// **私有构造器**：构造时自动设 `timestamp = System.currentTimeMillis()`

// **静态方法**：

// | 方法签名 | 逻辑 |
// |----------|------|
// | `static <T> Result<T> success()` | new Result(20000, "操作成功", null) |
// | `static <T> Result<T> success(T data)` | new Result(20000, "操作成功", data) |
// | `static <T> Result<T> success(String message, T data)` | new Result(20000, message, data) |
// | `static <T> Result<T> error()` | new Result(50000, "系统内部错误", null) |
// | `static <T> Result<T> error(String message)` | new Result(50000, message, null) |
// | `static <T> Result<T> error(ResultCode rc)` | new Result(rc.getCode(), rc.getMessage(), null) |
// | `static <T> Result<T> error(Integer code, String message)` | new Result(code, message, null) |
// | `boolean isSuccess()` | code != null && code == 20000 |
// 几个问题要改：

// 1. **类声明不对**：`private class Result` → 应该是 `public class Result<T> implements Serializable`（泛型类要在类名后加 `<T>`，而且是 public）
// 2. **私有构造器没写**：你直接 `new Result<>(...)`，但没有定义构造器，编译会报错
// 3. **timestamp 没赋值**：构造器里要写 `this.timestamp = System.currentTimeMillis()`
// 4. **注释删掉**，不要把题目注释留在代码里

// 改完重新贴。
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
