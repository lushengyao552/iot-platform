package com.example.library.common.exception;
// 题 1.3 BusinessException

import lombok.Getter;
import com.example.library.common.result.ResultCode;
// **文件**：`common/exception/BusinessException.java`

// ### 前置"为什么"
// > **为什么业务异常要继承 RuntimeException 而不是 Exception？**
// > 提示：如果继承 Exception，方法签名上要加什么？调用方会被迫做什么？

// ### 代码要求

// ```java
// // 继承 RuntimeException
// // 加 @Getter
// // 字段：private final Integer code;

// // 构造器1：BusinessException(ResultCode rc)
// //   super(rc.getMessage()); this.code = rc.getCode();

// // 构造器2：BusinessException(Integer code, String message)
// //   super(message); this.code = code;

// // 构造器3：BusinessException(String message)
// //   super(message); this.code = ResultCode.ERROR.getCode();
// ```
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

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }
    
}
