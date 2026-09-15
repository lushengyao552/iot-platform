# Stage 07：异常处理

## 阶段目标

建立完整的异常体系：业务异常 BizException、全局异常处理器 @RestControllerAdvice。把之前散落的 RuntimeException 全部替换掉。理解 checked vs unchecked、异常捕获顺序、栈信息。

## 前置要求

- stage06 毕业（有 Result 和 ResultCode）

## 本阶段知识点

- RuntimeException vs Exception
- 自定义 BizException
- @RestControllerAdvice + @ExceptionHandler
- 全局异常处理器要分层次：
  - BizException → 业务错误码
  - MethodArgumentNotValidException → 参数校验
  - AccessDeniedException → 权限
  - Exception → 兜底
- 异常不要吞，要打日志
- 不要在 Controller 里 try-catch 一堆
- 生产环境不要把堆栈返回前端

---

## 题目

### 【第 7-1 题：自定义业务异常】

**业务背景**
现在 Service 里到处 throw new RuntimeException，前端拿到的只是一坨 500。

**我的任务**
1. 写 `BizException extends RuntimeException`
   - 字段：int code
   - 构造器：(ResultCode code)、(int code, String message)
2. 写一个 ErrorCode 接口（可选，让 BizException 更通用）
3. 把 stage04 / stage05 里所有 throw new RuntimeException 改成 throw new BizException(ResultCode.XXX)

**限制条件**
- BizException 必须是 RuntimeException 子类（不要 checked）
- 不允许 catch 后 return null
- 异常 message 要对开发者有用，不要"出错了"三个字

**验收标准**
- 业务校验失败时抛 BizException
- 你能说清为什么业务异常用 RuntimeException 而不是 Exception
- 你能说清 throw new RuntimeException 和 throw new BizException 的区别

**思考题**
1. 为什么不推荐 throws Exception？
2. 异常应该在 Service 抛还是在 Controller 抛？
3. 异常消息要不要拼上具体业务值（比如"用户 zhangsan 不存在"）？安全吗？
4. 一个方法里抛多个异常，怎么组织？

---

### 【第 7-2 题：全局异常处理器】

**业务背景**
现在异常抛出去了，但 Spring 默认返回 500 白页。要统一兜住。

**我的任务**
1. 写 `GlobalExceptionHandler`，加 @RestControllerAdvice
2. 至少处理：
   - BizException → 返回 Result.failed(e.getCode(), e.getMessage())
   - MethodArgumentNotValidException → 参数校验错误
   - AccessDeniedException → 403
   - Exception → 兜底，记 error 日志，返回"系统繁忙"
3. 每个处理方法打日志（log.error / warn）

**限制条件**
- 兜底异常不要把堆栈返给前端
- 日志要打全：异常对象、请求路径、参数（注意脱敏）
- HTTP 状态码统一返回 200，业务 code 区分错误（也可以用 4xx/5xx，自己选一种并说明）

**验收标准**
- 故意触发业务异常，返回统一 Result
- 故意传错参数（配合 stage08），返回参数错误
- 故意抛个 NullPointerException，兜底处理器接住，前端看到"系统繁忙"
- 你能说清 @RestControllerAdvice 是怎么拦截到 Controller 异常的

**思考题**
1. @ControllerAdvice 和 @RestControllerAdvice 区别？
2. 异常处理器的匹配顺序？一个异常同时匹配多个 @ExceptionHandler 怎么办？
3. try-catch 里 throw 新异常，原异常怎么办（cause）？
4. 为什么不要在 Controller 里 try-catch 返回 Result？
5. 过滤器、拦截器里抛的异常，全局处理器接得到吗？（伏笔，stage09 会踩坑）

---

### 【第 7-3 题：异常分类与日志规范】

**业务背景**
不同异常要打不同级别的日志。

**我的任务**
1. BizException 打 warn（业务错误不算系统故障）
2. NullPointerException / SQLException 打 error
3. 参数校验错误打 info 或 debug
4. 日志里带上 traceId（先打个 UUID 占位，stage13 接入 MDC）

**限制条件**
- 不要 e.printStackTrace()
- 不要 log.error(e.getMessage())，要把异常对象传进去
- 不要把密码、身份证打到日志

**验收标准**
- 日志里能区分"用户输错了"和"系统炸了"
- 你能说清 log.error("xxx", e) 和 log.error(e.getMessage()) 的区别
- 你能说出 SLF4J 的占位符 {} 怎么用

**思考题**
1. 为什么 e.printStackTrace() 不行？
2. 同步异常和异步异常（@Async）在日志上有什么区别？
3. 生产环境日志级别怎么配？
4. 链路追踪 traceId 怎么传？

---

## 本阶段毕业考试

不看任何资料：
1. 给一段满是 try-catch + return null 的代码，让你重构成抛 BizException + 全局处理
2. 给你一个 NullPointerException，让你说该在哪一层兜、打什么日志
3. 让你现场写 GlobalExceptionHandler 并解释每个分支

## 本阶段反向面试题

1. Error 和 Exception 区别？
2. finally 块一定会执行吗？
3. try-with-resources 是干嘛的？
4. 自定义异常为什么要继承 RuntimeException？
5. Spring 的 @ExceptionHandler 原理？
6. 异常设计的"快失败"（fail-fast）原则？

## 常见坑

- 业务异常继承了 Exception（checked），到处 throws
- 兜底处理器把堆栈返回前端
- log.error(e.getMessage()) 丢了栈
- catch (Exception e) 后 return null，把问题吞了
- 过滤器/拦截器里的异常全局处理器接不到
